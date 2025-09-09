#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_DIR="$ROOT"
IMAGE_NAME="miniconcurexpense:dev"
K8S_NS="miniconcur"
USE_KIND=${USE_KIND:-false}   # set to true when using kind
USE_MINIKUBE=${USE_MINIKUBE:-true} # set to true when using minikube

echo "1/8  Building jar"
cd "$APP_DIR"
./mvnw -DskipTests package

echo "2/8  Building docker image: $IMAGE_NAME"
echo "Looking for Dockerfile at: $ROOT/Dockerfile"
ls -la "$ROOT/Dockerfile"
docker build -t "$IMAGE_NAME" -f "$ROOT/Dockerfile" "$ROOT"

if [[ "$USE_KIND" == "true" ]]; then
  echo "3/8  Loading image into kind cluster"
  kind load docker-image "$IMAGE_NAME"
elif [[ "$USE_MINIKUBE" == "true" ]]; then
  echo "3/8  Loading image into minikube"
  # Start minikube
  minikube start
  # Verify it's running
  kubectl cluster-info
  minikube image load "$IMAGE_NAME"
else
  echo "3/8  Not loading into local cluster (assumes cluster can pull image by this name)."
fi

echo "4/8  Ensure namespace exists"
kubectl get ns "$K8S_NS" >/dev/null 2>&1 || kubectl create ns "$K8S_NS"

echo "5/8  Apply secrets (idempotent)"
kubectl -n "$K8S_NS" apply -f k8s/namespace.yaml || true
kubectl -n "$K8S_NS" create secret generic db-credentials \
  --from-literal=POSTGRES_USER=app \
  --from-literal=POSTGRES_PASSWORD=secret \
  --dry-run=client -o yaml | kubectl apply -f -

echo "6/8  Apply k8s manifests"
kubectl -n "$K8S_NS" apply -f k8s/postgres-service.yaml
kubectl -n "$K8S_NS" apply -f k8s/postgres-statefulset.yaml
kubectl -n "$K8S_NS" apply -f k8s/redis-deployment.yaml
kubectl -n "$K8S_NS" apply -f k8s/redis-service.yaml

# Create the app deployment FIRST
echo "Applying app deployment"
kubectl -n "$K8S_NS" apply -f k8s/app-deployment.yaml

# THEN patch the image
echo "Patching app deployment image to $IMAGE_NAME"
kubectl -n "$K8S_NS" set image deployment/miniconcurexpense app="$IMAGE_NAME" --record=false
kubectl -n "$K8S_NS" apply -f k8s/app-service.yaml

echo "7/8  Wait for pods to be ready (timeout 180s)"
kubectl -n "$K8S_NS" wait --for=condition=available deployment/miniconcurexpense --timeout=180s || true
kubectl -n "$K8S_NS" wait --for=condition=ready pod -l app=postgres --timeout=180s || true
kubectl -n "$K8S_NS" wait --for=condition=ready pod -l app=redis --timeout=120s || true

echo "8/8  Deployment finished. Port-forwarding instructions:"
echo "  kubectl -n $K8S_NS port-forward svc/miniconcurexpense 8080:8080"
echo "Or open NodePort at node:30080 if using NodePort service"
