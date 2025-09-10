# Mini Concur — practising project (Java · Spring Boot · React · Vite · Kubernetes)

**Mini Concur** is a small practising project inspired by SAP Concur expense functionality. It is intentionally minimal and built for learning: Java (Spring Boot) backend, React frontend (Vite), Docker + Kubernetes deployment (manifests provided). The app implements a tiny "expenses" feature (list + add expenses).

> ⚠️ This is a learning/demo project — not production-ready. I Use it to experiment with Java, Spring Boot, React, Vite, Docker, minikube/kind and basic Kubernetes manifests.

---

## Repository layout (high level)

```
/frontend                      # React + Vite frontend
  ├─ src
  ├─ Dockerfile
  └─ nginx.conf (serves built assets & proxies /expenses -> backend)
k8s/                           # Kubernetes manifests (namespace, services, deployments)
Dockerfile                     # backend Dockerfile (root)
src/ (backend)                 # Spring Boot Java backend
build-and-deploy.ps1           # PowerShell script to build images and deploy to local k8s
README.md                      # <-- you are reading this
```

---

## Prerequisites / Dependencies

Install these on your development machine before attempting the full deploy script.

* **PowerShell 5+ (Windows) or PowerShell Core** — for the provided `build-and-deploy.ps1` powershell script.
* **Docker** — build images locally.
* **minikube** *or* **kind** (one of these) if you want a local Kubernetes cluster.

  * Script defaults to `USE_MINIKUBE = true` (minikube). You may export `USE_KIND=true` to use kind instead.
* **kubectl** — to interact with Kubernetes cluster.
* **Java JDK** — for building the backend with Maven (the project includes `mvnw` wrapper). JDK 11+ recommended.
* **Maven** — optional if you use the included `./mvnw` wrapper.
* **Node.js + npm** — to run/build the frontend locally (recommended Node 16+).

Make sure all of the above are available in your `PATH` before running the deploy script.
---

## Quickstart — deploy to local Kubernetes (minikube / kind)

> The repository contains `build-and-deploy.ps1` which automates building backend & frontend images, loading them into minikube/kind, applying Kubernetes manifests, patching deployments and port-forwarding.

Open **PowerShell** in the `scripts` folder and run:

```powershell
# default: uses minikube
.\build-and-deploy.ps1
```

### Using `kind` instead of `minikube`

Set an environment variable before running the script:

```powershell
$env:USE_KIND = "true"
$env:USE_MINIKUBE = "false"
.\build-and-deploy.ps1
```

### What the script does (summary)

1. Builds backend JAR using Maven (`./mvnw -DskipTests package`).
2. Builds backend Docker image.
3. Temporarily backs up `frontend/src/.env` (if present) to prevent `VITE_BACKEND_URL` from being inlined in the production build.
4. Builds frontend Docker image using a **unique timestamped tag** (prevents stale-image issues).
5. Loads images into minikube or kind (if configured).
6. Applies Kubernetes manifests in `k8s/`.
7. Patches deployments to use the newly built images.
8. Waits for deployments/pods to become ready and port-forwards the frontend service to `localhost:8081` (script prints instructions).

After the script finishes it prints suggested port-forwarding commands. By default it will also open a port-forward session for the frontend service to `localhost:8081` for you.

Access the app in your browser at:

```
http://localhost:8081
```

**Backend port-forward example (if you want direct backend):**

```powershell
kubectl -n miniconcur port-forward svc/miniconcurexpense 8080:8080
# backend API reachable at http://localhost:8080/expenses
```

---

## Run components locally (no Kubernetes)

If you prefer to run the backend and frontend locally (simpler for development):

### Backend (Spring Boot)

From repo root:

```bash
# Using the included maven wrapper
./mvnw -Dspring-boot.run.profiles=dev spring-boot:run
# or build and run jar:
./mvnw -DskipTests package
java -jar target/your-backend.jar
```

The backend listens on `8080` by default (see `application.properties`).

### Frontend (Vite dev server)

From `frontend/`:

```bash
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` by default. During development the frontend often talks directly to the backend (different origin) — see CORS section below or use the Vite proxy.

---

## Dev convenience: Vite proxy (avoid CORS while developing)

You can configure Vite to proxy API requests to the backend so the browser sees same-origin requests while developing. Example `vite.config.js` snippet:

```js
// vite.config.js
export default defineConfig({
  // ...
  server: {
    proxy: {
      '/expenses': { target: 'http://localhost:30080', changeOrigin: true },
      '/tenants':  { target: 'http://localhost:30080', changeOrigin: true },
      '/debug':    { target: 'http://localhost:30080', changeOrigin: true }
    }
  }
})
```

With that, running `npm run dev` will forward API calls to your backend and you don't need to enable CORS on the backend during dev.

---

## CORS — short guidance

* The backend contains `CorsConfig.java` which is currently permissive for convenience (allows `*`). That’s helpful during development but **not** recommended in production.
* **Recommended approach**: keep `CorsConfig` but enable it only for a `dev` profile (`@Profile("dev")`) or use the Vite proxy (above). If you always access the frontend through nginx inside the cluster (frontend served and proxying to backend), you can remove CORS config — but only if you are certain you will never run the frontend on a different origin.

---

## Troubleshooting

### 1) Browser still tries `http://localhost:30080` after deploying

This typically happened because Kubernetes was running an older image (big problem I had myself :D) that baked `VITE_BACKEND_URL=http://localhost:30080` into the frontend bundle.
But this localhost address was not the route to the backend and therefore was not working.

**Image Tag Confusion:**

Avoid reusing tags like `:dev` for images you actively change. The provided script uses unique timestamp tags to avoid that problem.

**How to check:**

```powershell
# show pod imageID
kubectl -n miniconcur get pod -l app=frontend -o jsonpath='{.items[0].status.containerStatuses[0].imageID}{"\n"}'

# check local image ID for a tag
docker images --format "{{.Repository}}:{{.Tag}} {{.ID}}" | Select-String "miniconcurexpense-frontend"

# grep inside the running pod for the literal URL
kubectl -n miniconcur exec -it deploy/frontend -- sh -c "grep -n -C3 'localhost:30080' /usr/share/nginx/html/assets/*.js || echo 'No match'"
```

**Fix:** Use the provided `build-and-deploy.ps1` (it creates a unique timestamped frontend image tag and loads that into minikube/kind). If you rebuild manually, ensure you run `minikube image load <tag>` (or `kind load docker-image <tag>`) and patch the deployment to that exact tag.


---

## Useful `kubectl` / debugging commands

```bash
# show deployment image
kubectl -n miniconcur get deploy frontend -o=jsonpath='{.spec.template.spec.containers[0].image}{"\n"}'

# show pod logs
kubectl -n miniconcur logs -l app=frontend --tail=200

# open a shell in the frontend pod
kubectl -n miniconcur exec -it deploy/frontend -- sh

# grep assets inside pod (checks for inlined localhost)
kubectl -n miniconcur exec -it deploy/frontend -- sh -c "grep -n -C3 'localhost:30080' /usr/share/nginx/html/assets/*.js || echo 'No match'"
```

---

## Tests / Quick sanity checks

* After running `build-and-deploy.ps1` and waiting for pods to be ready:

  * Open `http://localhost:8081` (or whatever port you forwarded) and try adding an expense.
  * Check browser DevTools Network tab for `/expenses` — it should be proxied by nginx (same origin) and not call `http://localhost:30080`.
  * If a network error shows `ERR_CONNECTION_REFUSED` for `localhost:30080`, the running frontend still contains an inlined dev URL (see troubleshooting above).

---

## Development tips & improvements

* Consider changing the frontend Dockerfile to accept a build `ARG` for `VITE_BACKEND_URL` and set `ENV` explicitly during build — this makes build-time values explicit and less error-prone.
* For production-like deployments, use a runtime config file served by nginx (e.g. `/config.json`) to avoid inlining environment-specific URLs at build time.
* Restrict CORS to specific origins and only enable it for development profile.

---

## License & notes

This repository is a practising/demo project — no warranty; use for learning only.
