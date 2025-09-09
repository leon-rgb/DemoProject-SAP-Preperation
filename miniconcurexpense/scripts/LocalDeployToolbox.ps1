# build-and-deploy.ps1
# Requires PowerShell 5+ (works well in PowerShell Core too)
# Stop on errors
$ErrorActionPreference = "Stop"

# Resolve paths
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$ROOT = Resolve-Path (Join-Path $SCRIPT_DIR "..")
$APP_DIR = $ROOT
$IMAGE_NAME = "miniconcurexpense:dev"
$K8S_NS = "miniconcur"

# Environment-like switches
if (-not $env:USE_KIND) { $env:USE_KIND = "false" }
if (-not $env:USE_MINIKUBE) { $env:USE_MINIKUBE = "true" }

Write-Host "1/8  Building jar"
Set-Location $APP_DIR
./mvnw -DskipTests package

Write-Host "2/8  Building docker image: $IMAGE_NAME"
Write-Host "Looking for Dockerfile at: $ROOT\Dockerfile"
Get-Item "$ROOT\Dockerfile" | Format-List
docker build -t $IMAGE_NAME -f "$ROOT\Dockerfile" "$ROOT"

if ($env:USE_KIND -eq "true") {
    Write-Host "3/8  Loading image into kind cluster"
    kind load docker-image $IMAGE_NAME
}
elseif ($env:USE_MINIKUBE -eq "true") {
    Write-Host "3/8  Loading image into minikube"
    # minikube start
    kubectl cluster-info
    minikube image load $IMAGE_NAME
}
else {
    Write-Host "3/8  Not loading into local cluster (assumes cluster can pull image by this name)."
}

Write-Host "4/8  Ensure namespace exists"
try {
    kubectl get ns $K8S_NS *> $null 2>&1
} catch {
    Write-Host "Namespace $K8S_NS not found. Creating..."
    kubectl create ns $K8S_NS
}

Write-Host "5/8  Apply secrets (idempotent)"
kubectl -n $K8S_NS apply -f k8s/namespace.yaml
kubectl -n $K8S_NS create secret generic db-credentials `
    --from-literal=POSTGRES_USER=app `
    --from-literal=POSTGRES_PASSWORD=secret `
    --dry-run=client -o yaml |
kubectl apply -f -

Write-Host "6/8  Apply k8s manifests"
Set-Location $ROOT
kubectl -n $K8S_NS apply -f k8s/postgres-service.yaml
kubectl -n $K8S_NS apply -f k8s/postgres-statefulset.yaml
kubectl -n $K8S_NS apply -f k8s/redis-deployment.yaml
kubectl -n $K8S_NS apply -f k8s/redis-service.yaml

Write-Host "Applying app deployment"
kubectl -n $K8S_NS apply -f k8s/app-deployment.yaml

Write-Host "Patching app deployment image to $IMAGE_NAME"
kubectl -n $K8S_NS set image deployment/miniconcurexpense app=$IMAGE_NAME --record=false
kubectl -n $K8S_NS apply -f k8s/app-service.yaml

Write-Host "7/8  Wait for pods to be ready (timeout 180s)"
kubectl -n $K8S_NS wait --for=condition=available deployment/miniconcurexpense --timeout=180s
kubectl -n $K8S_NS wait --for=condition=ready pod -l app=postgres --timeout=180s
kubectl -n $K8S_NS wait --for=condition=ready pod -l app=redis --timeout=120s

Write-Host "8/8  Deployment finished. Port-forwarding instructions:"
Write-Host "  kubectl -n $K8S_NS port-forward svc/miniconcurexpense 8080:8080"
Write-Host "Or open NodePort at node:30080 if using NodePort service"
