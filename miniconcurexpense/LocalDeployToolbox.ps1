# build-and-deploy.ps1
# Requires PowerShell 5+ (works in PowerShell Core too)
# Stop on errors
$ErrorActionPreference = "Stop"

# Resolve paths
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$ROOT = $SCRIPT_DIR
$APP_DIR = $ROOT
$FRONTEND_DIR = Join-Path $ROOT "frontend"

# images (backend kept as simple dev tag, frontend will get a timestamped tag)
$BACKEND_IMAGE = "miniconcurexpense-backend:dev"
$TIMESTAMP = Get-Date -Format "yyyyMMddHHmmss"
$FRONTEND_IMAGE = "miniconcurexpense-frontend:dev-$TIMESTAMP"

$K8S_NS = "miniconcur"

# Environment-like switches (defaults)
if (-not $env:USE_KIND)  { $env:USE_KIND = "false" }
if (-not $env:USE_MINIKUBE) { $env:USE_MINIKUBE = "true" }

# --- Helper: backup frontend .env if present (prevent inlining localhost) ---
$frontendEnv = Join-Path $FRONTEND_DIR "src\.env"
$frontendEnvBackup = Join-Path $FRONTEND_DIR "src\.env.development"
$envBackedUp = $false

if (Test-Path $frontendEnv) {
    Write-Host "Backing up frontend .env -> .env.development to avoid inlining dev URL into production build"
    Move-Item -Force $frontendEnv $frontendEnvBackup
    $envBackedUp = $true
}

try {
    # --- Backend Build ---
    Write-Host "1/10  Building backend jar"
    Set-Location $APP_DIR
    ./mvnw -DskipTests package

    Write-Host "2/10  Building backend docker image: $BACKEND_IMAGE"
    docker build -t $BACKEND_IMAGE -f "$ROOT\Dockerfile" "$ROOT"

    # --- Frontend Build ---
    Write-Host "3/10  Building frontend docker image: $FRONTEND_IMAGE"
    Set-Location $FRONTEND_DIR

    # clean builder cache lightly and remove any old local dev-tag image if present
    docker builder prune -f

    # Build with no cache and unique tag to guarantee freshness
    docker build --no-cache -t $FRONTEND_IMAGE -f "$FRONTEND_DIR\Dockerfile" "$FRONTEND_DIR"

    # --- Load images into cluster ---
    if ($env:USE_KIND -eq "true") {
        Write-Host "4/10  Loading images into kind cluster"
        kind load docker-image $BACKEND_IMAGE
        kind load docker-image $FRONTEND_IMAGE
    }
    elseif ($env:USE_MINIKUBE -eq "true") {
        Write-Host "4/10  Loading images into minikube"
        kubectl cluster-info
        minikube image load $BACKEND_IMAGE
        minikube image load $FRONTEND_IMAGE
    }
    else {
        Write-Host "4/10  Not loading into local cluster (assumes cluster can pull image by this name)."
    }

    # --- Ensure namespace exists ---
    Write-Host "5/10  Ensure namespace exists"
    try {
        kubectl get ns $K8S_NS *> $null 2>&1
    } catch {
        Write-Host "Namespace $K8S_NS not found. Creating..."
        kubectl create ns $K8S_NS
    }

    # --- Apply secrets (idempotent) ---
    Write-Host "6/10  Apply secrets and namespace manifest (idempotent)"
    kubectl -n $K8S_NS apply -f k8s/namespace.yaml
    kubectl -n $K8S_NS create secret generic db-credentials `
        --from-literal=POSTGRES_USER=app `
        --from-literal=POSTGRES_PASSWORD=secret `
        --dry-run=client -o yaml |
    kubectl apply -f -

    # --- Apply k8s manifests ---
    Write-Host "7/10  Apply k8s manifests"
    Set-Location $ROOT
    kubectl -n $K8S_NS apply -f k8s/postgres-service.yaml
    kubectl -n $K8S_NS apply -f k8s/postgres-statefulset.yaml
    kubectl -n $K8S_NS apply -f k8s/redis-deployment.yaml
    kubectl -n $K8S_NS apply -f k8s/redis-service.yaml

    kubectl -n $K8S_NS apply -f k8s/frontend-deployment.yaml
    kubectl -n $K8S_NS apply -f k8s/frontend-service.yaml

    Write-Host "Applying backend deployment"
    kubectl -n $K8S_NS apply -f k8s/app-deployment.yaml

    Write-Host "Patching backend deployment image to $BACKEND_IMAGE"
    kubectl -n $K8S_NS set image deployment/miniconcurexpense app=$BACKEND_IMAGE --record=false

    Write-Host "Patching frontend deployment image to $FRONTEND_IMAGE"
    # crucial: set the exact unique tag we just built and loaded
    kubectl -n $K8S_NS set image deployment/frontend frontend=$FRONTEND_IMAGE --record=false

    kubectl -n $K8S_NS apply -f k8s/app-service.yaml

    # --- Wait for pods to be ready ---
    Write-Host "8/10  Wait for pods to be ready (timeout 180s)"
    kubectl -n $K8S_NS wait --for=condition=available deployment/miniconcurexpense --timeout=180s
    kubectl -n $K8S_NS wait --for=condition=available deployment/frontend --timeout=180s
    kubectl -n $K8S_NS wait --for=condition=ready pod -l app=postgres --timeout=180s
    kubectl -n $K8S_NS wait --for=condition=ready pod -l app=redis --timeout=120s

    # --- Optional: restart frontend to ensure new image picked up immediately ---
    Write-Host "9/10  Restart frontend rollout to ensure pods are re-created with the new image"
    kubectl -n $K8S_NS rollout restart deployment/frontend
    kubectl -n $K8S_NS rollout status deployment/frontend

    # --- Port-forwarding ---
    Write-Host "10/10  Deployment finished. Port-forwarding instructions:"
    Write-Host "  Backend: kubectl -n $K8S_NS port-forward svc/miniconcurexpense 8080:8080"
    Write-Host "  Frontend: kubectl -n $K8S_NS port-forward svc/frontend 8081:80"
    kubectl -n $K8S_NS port-forward svc/frontend 8081:80
}
finally {
    # restore .env if we backed it up
    if ($envBackedUp -and (Test-Path $frontendEnvBackup)) {
        Write-Host "Restoring frontend .env from .env.development"
        Move-Item -Force $frontendEnvBackup $frontendEnv
    }
}
