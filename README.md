# Flink Training 2.x - Exercises

This is a custom training project for Apache Flink 2.x, providing hands-on exercises
to learn core Flink concepts. It follows the same structure as the official
[apache/flink-training](https://github.com/apache/flink-training) repository but
updated for Flink 2.x APIs.

## Prerequisites

- JDK 17 or higher
- Gradle 8.x (wrapper included)
- Apache Flink 2.0+ (optional, for cluster deployment)

## Building

```bash
./gradlew build
```

## Exercises

### Exercise 1: Ride Cleansing (Filtering)
**Location:** `ride-cleansing/`

**Concepts:**
- DataStream filtering with `filter()`
- Geographic data validation
- Basic transformations

**Run Solution:**
```bash
java -cp ride-cleansing/build/libs/ride-cleansing-2.0-SNAPSHOT-all.jar \
    org.apache.flink.training.exercises.ridecleansing.RideCleansingSolution
```

### Exercise 2: Rides and Fares (Stateful Enrichment)
**Location:** `rides-and-fares/`

**Concepts:**
- Connected streams with `connect()`
- Keyed state with `ValueState`
- State buffering for out-of-order events
- `RichCoFlatMapFunction`

**Run Solution:**
```bash
java -cp rides-and-fares/build/libs/rides-and-fares-2.0-SNAPSHOT-all.jar \
    org.apache.flink.training.exercises.ridesandfares.RidesAndFaresSolution
```

### Exercise 3: Hourly Tips (Windowed Analytics)
**Location:** `hourly-tips/`

**Concepts:**
- Event time processing
- Watermarks
- Tumbling windows
- `AggregateFunction`
- `ProcessWindowFunction`

### Exercise 4: Long Ride Alerts (ProcessFunction & Timers)
**Location:** `long-ride-alerts/`

**Concepts:**
- `KeyedProcessFunction`
- Event-time timers
- State management
- Timer callbacks with `onTimer()`

**Run Solution:**
```bash
java -cp long-ride-alerts/build/libs/long-ride-alerts-2.0-SNAPSHOT-all.jar \
    org.apache.flink.training.exercises.longridealerts.LongRideAlertsSolution
```

## Project Structure

```
flink-training-2x/
├── common/                    # Shared data types and generators
│   └── src/main/java/
│       └── org/apache/flink/training/
│           ├── datatypes/     # TaxiRide, TaxiFare
│           ├── sources/       # Data generators
│           └── utils/         # GeoUtils
├── ride-cleansing/           # Exercise 1
├── rides-and-fares/          # Exercise 2
├── hourly-tips/              # Exercise 3
└── long-ride-alerts/         # Exercise 4
```

## Key API Changes from Flink 1.x

1. **Source API**: Uses `DataGeneratorSource` instead of deprecated `SourceFunction`
2. **open() method**: Uses `OpenContext` instead of `Configuration`
3. **fromData()**: Replaces `fromElements()` for in-memory data
4. **FileSource**: New unified file reading API

## Learning Path

1. Start with **Ride Cleansing** - learn basic filtering
2. Move to **Rides and Fares** - understand stateful processing
3. Try **Hourly Tips** - master windowed analytics
4. Complete **Long Ride Alerts** - learn ProcessFunction and timers

---

# Production: Real-Time Order Analytics Dashboard

The `sql-dashboard` exercise has been promoted to a full production system on Azure.
An order generator produces synthetic orders into Azure Event Hubs; Apache Flink SQL
computes four analytics windows; a FastAPI backend serves results over REST and WebSocket;
a Next.js dashboard renders live charts in the browser.

## Architecture

```
Order Generator (Python)
        │  JSON → orders-raw (10 ord/s)
        ▼
Azure Event Hubs (Premium, Kafka protocol)
        │
        ▼
Flink SQL Job (AKS, Flink Kubernetes Operator)
  ├─ TUMBLE(1m)    → orders-by-region
  ├─ HOP(30s/1m)   → top-products
  ├─ TUMBLE(1m)    → revenue-by-category
  └─ FILTER(>$500) → high-value-alerts
        │
        ▼
Dashboard Backend (FastAPI + aiokafka)
  ├─ Writes to TimescaleDB (hypertables)
  ├─ REST /api/analytics/*  ← historical queries
  └─ WebSocket /ws/stream   ← live push to browser
        │
        ▼
Dashboard Frontend (Next.js 14 + ECharts)
  2×2 panel grid, auto-reconnecting WebSocket
```

---

## Azure Resources

### Subscription & Tenant

| Field | Value |
|---|---|
| Subscription | Visual Studio Enterprise Subscription |
| Subscription ID | `b4ba478b-f888-443a-9694-bc12702e2310` |
| Tenant ID | `86a61b1c-2773-4d47-af70-25ff6096d6fe` |
| Region | East US 2 (`eastus2`) |
| Resource Group | `prod-eus2-orders-rg` |

### AKS Cluster

| Field | Value |
|---|---|
| Name | `prod-eus2-orders-aks` |
| Kubernetes version | 1.33.6 |
| API FQDN | `prod-eus2-orders-aks-3ns632mk.hcp.eastus2.azmk8s.io` |

**Node pools:**

| Pool | VM Size | Nodes | Purpose |
|---|---|---|---|
| system | Standard_D2s_v3 | 2 (fixed) | Kubernetes system services |
| flink | Standard_D4s_v3 | 2–6 (autoscale) | Flink JobManager + TaskManagers |
| app | Standard_D2s_v3 | 2–4 (autoscale) | Backend, frontend, TimescaleDB |

### Azure Container Registry

| Field | Value |
|---|---|
| Name | `prodeus2ordersacr` |
| Login server | `prodeus2ordersacr.azurecr.io` |
| SKU | Premium |

### Azure Event Hubs

| Field | Value |
|---|---|
| Namespace | `prodeus2ordersehns` |
| SKU | Premium (1 CU) |
| Kafka endpoint | `prodeus2ordersehns.servicebus.windows.net:9093` |
| Protocol | Kafka (SASL_SSL + `$ConnectionString`) |

**Topics (4 partitions, 7-day retention each):**

| Topic | Producer | Consumer |
|---|---|---|
| `orders-raw` | order-generator | Flink SQL job |
| `orders-by-region` | Flink SQL job | dashboard-backend |
| `top-products` | Flink SQL job | dashboard-backend |
| `revenue-by-category` | Flink SQL job | dashboard-backend |
| `high-value-alerts` | Flink SQL job | dashboard-backend |

### Azure Key Vault

| Field | Value |
|---|---|
| Name | `prod-eus2-orders-kv` |
| Vault URI | `https://prod-eus2-orders-kv.vault.azure.net/` |
| SKU | Standard, RBAC authorization |

**Secrets stored:**

| Secret name | Contents |
|---|---|
| `eventhub-conn-string` | Event Hubs SAS connection string (send + listen) |
| `eventhub-namespace` | `prodeus2ordersehns` |

### Azure Storage (Flink State Backend)

| Field | Value |
|---|---|
| Account | `prodeus2ordersstorage` |
| DFS endpoint | `https://prodeus2ordersstorage.dfs.core.windows.net/` |
| SKU | Standard LRS |
| Flink checkpoints | `flink-state` container |
| Flink savepoints | `flink-savepoints` container |

### Terraform State Storage

| Field | Value |
|---|---|
| Resource group | `prodeus2orderstfstate-rg` |
| Storage account | `prodeus2orderstfstate` |
| Container | `tfstate` |
| State file | `orders-dashboard.tfstate` |

### GitHub Actions Service Principal

| Field | Value |
|---|---|
| Display name | `flink-orders-github-actions` |
| App (Client) ID | `36fe3826-38e4-42c9-a80a-37e294e8a8a7` |
| Service Principal Object ID | `8eba3950-7d20-4bd0-b26f-950b04f99fee` |
| Auth method | OIDC Federated Identity (no stored secrets) |
| Roles | Contributor, User Access Administrator, Key Vault Secrets Officer |

---

## Kubernetes Cluster Components

### Namespaces

| Namespace | Purpose |
|---|---|
| `flink-operator` | Flink Kubernetes Operator controller |
| `flink` | Flink jobs (FlinkDeployment CRs) |
| `orders` | App workloads (generator, backend, frontend, TimescaleDB) |
| `monitoring` | Prometheus, Grafana, Alertmanager |
| `cert-manager` | TLS certificate automation |
| `ingress-nginx` | NGINX Ingress Controller |

### Helm Releases

| Release | Namespace | Chart | App Version |
|---|---|---|---|
| `cert-manager` | cert-manager | cert-manager-v1.19.4 | v1.19.4 |
| `ingress-nginx` | ingress-nginx | ingress-nginx-4.14.3 | 1.14.3 |
| `kube-prometheus-stack` | monitoring | 82.4.3 | v0.89.0 (Prometheus) |
| `flink-kubernetes-operator` | flink-operator | 1.10.0 | 1.10.0 |

### Public Endpoints

| Service | Address |
|---|---|
| NGINX Ingress (HTTP/HTTPS) | `4.152.201.248` |
| Dashboard frontend | `http://4.152.201.248` (after app deploy) |
| Grafana (port-forward) | `http://localhost:3000` |

Access Grafana locally:
```bash
kubectl port-forward svc/kube-prometheus-stack-grafana 3000:80 -n monitoring
# http://localhost:3000  admin / admin123
```

---

## Networking

| Resource | CIDR |
|---|---|
| VNet | `10.0.0.0/16` |
| AKS subnet | `10.0.0.0/20` |
| Private endpoints subnet | `10.0.16.0/24` |
| Storage subnet | `10.0.17.0/24` |
| Kubernetes service CIDR | `10.100.0.0/16` |
| DNS service IP | `10.100.0.10` |

AKS subnet has service endpoints for `Microsoft.KeyVault`, `Microsoft.ContainerRegistry`,
and `Microsoft.EventHub`.

---

## Production Setup Process

### Prerequisites

- Azure CLI (`az`) ≥ 2.60
- Terraform ≥ 1.9
- kubectl ≥ 1.33
- Helm ≥ 3.15
- GitHub CLI (`gh`)

### Step 1 — Terraform State Backend (run once manually)

```bash
az group create --name prodeus2orderstfstate-rg --location eastus2
az storage account create \
  --name prodeus2orderstfstate \
  --resource-group prodeus2orderstfstate-rg \
  --sku Standard_LRS --kind StorageV2
az storage container create \
  --name tfstate --account-name prodeus2orderstfstate
```

### Step 2 — GitHub Actions OIDC Service Principal

```bash
az ad sp create-for-rbac \
  --name "flink-orders-github-actions" \
  --role Contributor \
  --scopes /subscriptions/b4ba478b-f888-443a-9694-bc12702e2310

# Grant role assignment permissions (needed by Terraform)
az role assignment create \
  --assignee 8eba3950-7d20-4bd0-b26f-950b04f99fee \
  --role "User Access Administrator" \
  --scope /subscriptions/b4ba478b-f888-443a-9694-bc12702e2310

# Federated credentials (replace GITHUB_ORG/REPO as needed)
az ad app federated-credential create --id 36fe3826-38e4-42c9-a80a-37e294e8a8a7 --parameters '{
  "name":"github-main","issuer":"https://token.actions.githubusercontent.com",
  "subject":"repo:slxswaa1993/flink-training-2x:ref:refs/heads/main",
  "audiences":["api://AzureADTokenExchange"]}'

az ad app federated-credential create --id 36fe3826-38e4-42c9-a80a-37e294e8a8a7 --parameters '{
  "name":"github-pr","issuer":"https://token.actions.githubusercontent.com",
  "subject":"repo:slxswaa1993/flink-training-2x:pull_request",
  "audiences":["api://AzureADTokenExchange"]}'
```

Add these GitHub repository secrets (`Settings → Secrets → Actions`):

| Secret | Value |
|---|---|
| `AZURE_CLIENT_ID` | `36fe3826-38e4-42c9-a80a-37e294e8a8a7` |
| `AZURE_TENANT_ID` | `86a61b1c-2773-4d47-af70-25ff6096d6fe` |
| `AZURE_SUBSCRIPTION_ID` | `b4ba478b-f888-443a-9694-bc12702e2310` |

### Step 3 — Provision Azure Infrastructure

Trigger the `deploy-infrastructure.yml` workflow (manual dispatch), or run locally:

```bash
cd infra/terraform
terraform init
terraform plan -out tfplan
terraform apply tfplan
```

After apply, grant the SP Key Vault secrets access:

```bash
KV_ID=$(az keyvault show --name prod-eus2-orders-kv \
  --resource-group prod-eus2-orders-rg --query id -o tsv)
az role assignment create \
  --assignee 8eba3950-7d20-4bd0-b26f-950b04f99fee \
  --role "Key Vault Secrets Officer" --scope $KV_ID
```

### Step 4 — Configure kubectl

```bash
az aks get-credentials \
  --name prod-eus2-orders-aks \
  --resource-group prod-eus2-orders-rg

# On WSL — credentials are written to the Windows kubeconfig path
export KUBECONFIG=/mnt/c/Users/<your-windows-user>/.kube/config
kubectl get nodes
```

### Step 5 — Install Cluster Add-ons

```bash
# Namespaces
for ns in flink flink-operator orders monitoring cert-manager ingress-nginx; do
  kubectl create namespace $ns
done

# Helm repos
helm repo add jetstack https://charts.jetstack.io
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# cert-manager
helm install cert-manager jetstack/cert-manager \
  --namespace cert-manager --set crds.enabled=true --wait

# NGINX Ingress
helm install ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx --set controller.replicaCount=2 --wait

# Prometheus + Grafana + Alertmanager
helm install kube-prometheus-stack prometheus-community/kube-prometheus-stack \
  --namespace monitoring --set grafana.adminPassword=admin123 --wait

# Flink Kubernetes Operator 1.10.0
# Note: ghcr.io OCI registry requires authentication; use Apache archive tarball
curl -L -o /tmp/flink-operator-helm.tgz \
  "https://archive.apache.org/dist/flink/flink-kubernetes-operator-1.10.0/flink-kubernetes-operator-1.10.0-helm.tgz"
helm install flink-kubernetes-operator /tmp/flink-operator-helm.tgz \
  --namespace flink-operator --set webhook.create=false --wait
```

### Step 6 — Build and Push Docker Images

Push to `main` to trigger `build-and-push.yml`, which pushes four images to ACR:

```
prodeus2ordersacr.azurecr.io/flink-sql-dashboard:<sha>
prodeus2ordersacr.azurecr.io/order-generator:<sha>
prodeus2ordersacr.azurecr.io/dashboard-backend:<sha>
prodeus2ordersacr.azurecr.io/dashboard-frontend:<sha>
```

### Step 7 — Deploy Application Services

`deploy-app.yml` runs automatically after a successful image build:

```bash
# Manual deploy
kubectl apply -k k8s/overlays/production
kubectl apply -f k8s/monitoring/

# Verify
kubectl get pods -n orders
kubectl get pods -n flink
kubectl get flinkdeployment -n flink
```

---

## Application Services

Three services run alongside the Flink job in the `orders` namespace:

### Order Generator (`services/order-generator/`)

Python service that produces synthetic orders to Event Hubs at ~10 orders/sec.

- **Language**: Python 3.12 + `confluent-kafka`
- **Realistic patterns**: sinusoidal time-of-day rate (morning/evening peaks), regional
  weighting (NA 35%, EU 25%, APAC 25%, other 15%), random 3× burst spikes every ~10 min
- **Connects to**: Event Hubs topic `orders-raw` via SASL_SSL + `$ConnectionString`
- **Config**: `EVENTHUB_NAMESPACE` and `EVENTHUB_CONN_STRING` from Key Vault (same secrets as Flink job)

### Dashboard Backend (`services/dashboard-backend/`)

FastAPI service that bridges Event Hubs → TimescaleDB → browser.

- **Language**: Python 3.12, FastAPI 0.115, `aiokafka`, `asyncpg`
- **Data flow**: `aiokafka` consumer reads all 4 output topics → asyncio queue → fan-out to
  WebSocket clients + insert rows into TimescaleDB hypertables
- **REST API** (historical data):

  | Endpoint | Description |
  |---|---|
  | `GET /api/analytics/region?minutes=30` | Orders by region, last N minutes |
  | `GET /api/analytics/products?minutes=30` | Top products history |
  | `GET /api/analytics/category?minutes=30` | Revenue by category |
  | `GET /api/analytics/alerts?limit=50` | Recent high-value alerts |
  | `GET /health` | Liveness / readiness probe |
  | `GET /metrics` | Prometheus metrics |

- **WebSocket**: `WS /ws/stream` — all 4 topics multiplexed, pushed live to connected browsers

### Dashboard Frontend (`services/dashboard-frontend/`)

Next.js 14 app with a 2×2 panel grid of live-updating charts.

- **Stack**: Next.js 14 (App Router), TypeScript, Tailwind CSS, ECharts, SWR
- **Layout**:
  ```
  ┌──────────────────────┬──────────────────────┐
  │  Orders by Region    │  Top 3 Products       │
  │  Stacked bar chart   │  Horizontal bar       │
  ├──────────────────────┼──────────────────────┤
  │  Revenue by Category │  High-Value Alerts    │
  │  Multi-line chart    │  Scrolling table      │
  └──────────────────────┴──────────────────────┘
  ```
- **Initial load**: SWR fetches REST endpoints for historical data
- **Live updates**: WebSocket connects to `/ws/stream`, reconnects automatically on drop
- **Why ECharts over Recharts**: Canvas-based rendering handles high-frequency WebSocket
  updates without React reconciler bottleneck

---

## Secrets Flow

No secrets are stored in Git or Kubernetes YAML. The full chain:

```
Azure Key Vault (prod-eus2-orders-kv)
  ├─ eventhub-conn-string  "Endpoint=sb://prodeus2ordersehns..."
  └─ eventhub-namespace    "prodeus2ordersehns"
          │
          │  Key Vault CSI Driver (AKS add-on, enabled via Terraform)
          │  SecretProviderClass: k8s/base/orders/secret-provider-class.yaml
          ▼
  Kubernetes Secret: eventhub-secrets  (in flink + orders namespaces)
    ├─ key: connection-string
    └─ key: namespace
          │
          │  envFrom / secretKeyRef in pod spec
          ▼
  Pod environment variables
    ├─ EVENTHUB_CONN_STRING
    └─ EVENTHUB_NAMESPACE
          │
          │  JobConfig.java / config.py reads at startup
          ▼
  Kafka SASL_SSL connection to Event Hubs
```

The AKS kubelet identity is granted **Key Vault Secrets User** role by Terraform,
so pods can pull secrets without any credentials in their YAML.

---

## Docker Images

Four images are built and stored in ACR. Each gets two tags on every build:
- `:<git-sha>` — immutable, used for deployments (enables precise rollbacks)
- `:latest` — floating, for convenience

| Image | Source | Dockerfile | Main class / entrypoint |
|---|---|---|---|
| `flink-sql-dashboard` | `sql-dashboard/` | `infra/docker/flink-job/Dockerfile` | `SqlDashboardJob` (fat JAR via `shadowJar`) |
| `order-generator` | `services/order-generator/` | `infra/docker/order-generator/Dockerfile` | `python main.py` |
| `dashboard-backend` | `services/dashboard-backend/` | `infra/docker/dashboard-backend/Dockerfile` | `uvicorn app.main:app` |
| `dashboard-frontend` | `services/dashboard-frontend/` | `infra/docker/dashboard-frontend/Dockerfile` | `nginx` serving Next.js static export |

Build cache is stored in ACR itself (`type=registry` cache) — subsequent builds only
rebuild changed layers, keeping CI times fast.

---

## CI/CD Pipelines

### Authentication — how all pipelines connect to Azure

All 4 workflows use **OIDC Federated Identity** — no client secrets stored anywhere.
GitHub generates a short-lived JWT for each run; Azure exchanges it for an access token:

```yaml
- uses: azure/login@v2
  with:
    client-id: ${{ secrets.AZURE_CLIENT_ID }}       # 36fe3826-38e4-42c9-a80a-37e294e8a8a7
    tenant-id: ${{ secrets.AZURE_TENANT_ID }}       # 86a61b1c-2773-4d47-af70-25ff6096d6fe
    subscription-id: ${{ secrets.AZURE_SUBSCRIPTION_ID }}
```

Federated credentials on the SP restrict auth to: pushes to `main` and pull requests
on `slxswaa1993/flink-training-2x` only.

---

### 1. `pr-checks.yml` — Safety gate on every PR

**Trigger**: any Pull Request targeting `main`

Runs 3 jobs in parallel — the PR cannot be merged if any fail:

```
PR opened
  ├─ python-lint (matrix: order-generator + dashboard-backend)
  │    ruff check + mypy type check
  │
  ├─ frontend-build
  │    npm ci + next build  (catches TypeScript / import errors)
  │
  └─ terraform-plan
       terraform init + plan -no-color
       uploads tfplan as a workflow artifact for review
       (uses 'staging' environment for OIDC)
```

Nothing is deployed. Output: pass/fail on the PR status checks.

---

### 2. `build-and-push.yml` — Build all Docker images

**Trigger**: push to `main`, or manual dispatch (`workflow_dispatch`)

Runs 4 parallel jobs via a build matrix — one per image:

```
push to main
  ├─ Build flink-sql-dashboard   → prodeus2ordersacr.azurecr.io/flink-sql-dashboard:<sha> + :latest
  ├─ Build order-generator       → prodeus2ordersacr.azurecr.io/order-generator:<sha> + :latest
  ├─ Build dashboard-backend     → prodeus2ordersacr.azurecr.io/dashboard-backend:<sha> + :latest
  └─ Build dashboard-frontend    → prodeus2ordersacr.azurecr.io/dashboard-frontend:<sha> + :latest
```

Each job: `az login (OIDC)` → `az acr login` → `docker buildx build --push`

Cache strategy: `cache-from/cache-to type=registry` stores Docker layer cache in ACR,
so only changed layers are rebuilt on subsequent runs.

---

### 3. `deploy-app.yml` — Deploy to AKS

**Trigger**: automatically when `build-and-push.yml` completes successfully on `main`;
or manually via `workflow_dispatch` (lets you specify any image tag)

```
build-and-push succeeded
  │
  ▼
1. az login (OIDC)
2. az aks get-credentials  →  kubectl access to prod-eus2-orders-aks
3. kubectl apply -k k8s/overlays/production --server-side
       applies all manifests: Deployments, StatefulSet, Services,
       HPAs, Ingress, SecretProviderClass, FlinkDeployment
4. kubectl set image  →  patch order-generator / backend / frontend to :<git-sha>
5. kubectl patch flinkdeployment sql-dashboard  →  patch to :<git-sha>
       (triggers Flink Operator savepoint upgrade — no data loss)
6. kubectl rollout status  →  waits up to 5 min for pods to go healthy
7. kubectl get pods -n orders + get flinkdeployment -n flink  →  final status print
```

The `if:` condition ensures this job **only runs if the build succeeded** —
a broken image build never reaches the cluster.

**Manual trigger** (useful for rolling back or redeploying a specific SHA):
```
GitHub → Actions → Deploy Application → Run workflow → image_tag: <sha>
```

---

### 4. `deploy-infrastructure.yml` — Terraform

**Trigger**: push to `main` that changes `infra/terraform/**` (auto-apply), or
manual dispatch where you choose `plan` or `apply`

```
infra/terraform/** changed (or manual trigger)
  │
  ▼
terraform init    →  connects to blob state (prodeus2orderstfstate / tfstate)
terraform plan    →  always runs, shows what will change
terraform apply   →  only if: action=apply OR push to main
terraform output  →  prints resource values after apply
```

Uses `production` environment for apply (requires OIDC credential with Contributor +
User Access Administrator roles), `staging` environment for plan-only runs.

---

### End-to-end flow

```
Developer opens PR
        │
        └─ pr-checks.yml  (lint + build check + tf plan)  ← blocks merge if failing
                │
Developer merges to main
        │
        ├─ build-and-push.yml  →  4 images → ACR
        │         │
        │         └─ (on success) deploy-app.yml  →  AKS rolling deploy
        │
        └─ (only if infra/terraform/** changed)
           deploy-infrastructure.yml  →  terraform apply
```

---

## Monitoring & Alerts

**Prometheus rules** (`k8s/monitoring/prometheus-rules.yaml`):

| Alert | Condition | Severity |
|---|---|---|
| `FlinkJobNotRunning` | Job ≠ RUNNING for 2 min | critical |
| `FlinkCheckpointFailing` | Checkpoint failures > 0 for 5 min | warning |
| `KafkaConsumerLag` | Lag > 5 000 msgs for 3 min | warning |
| `BackendHighLatency` | p99 latency > 500 ms | warning |
| `EventHubThrottled` | Throttle errors > 0 for 1 min | critical |
| `NodeCPUHigh` | CPU > 85% for 5 min | warning |

**Grafana dashboards** in `k8s/monitoring/grafana-dashboards/`:
- `flink-health.json` — job status, checkpoint duration, backpressure
- `order-analytics.json` — business KPIs (revenue, top products, alert frequency)

---

## Day-2 Operations

### Check what's running

```bash
# All pods across the system
kubectl get pods -n orders
kubectl get pods -n flink
kubectl get pods -n flink-operator
kubectl get pods -n monitoring

# Flink job status  (look for STABLE / RUNNING)
kubectl get flinkdeployment -n flink

# Ingress public IP
kubectl get svc ingress-nginx-controller -n ingress-nginx
```

### Tail logs

```bash
# Flink JobManager
kubectl logs -n flink -l component=jobmanager -f

# Order generator
kubectl logs -n orders -l app=order-generator -f

# Dashboard backend
kubectl logs -n orders -l app=dashboard-backend -f
```

### Redeploy a specific image SHA (rollback)

```bash
# Go to GitHub Actions → Deploy Application → Run workflow → enter the old SHA
# Or manually:
kubectl set image deployment/dashboard-backend \
  dashboard-backend=prodeus2ordersacr.azurecr.io/dashboard-backend:<old-sha> -n orders

kubectl patch flinkdeployment sql-dashboard -n flink \
  --type json \
  -p '[{"op":"replace","path":"/spec/image","value":"prodeus2ordersacr.azurecr.io/flink-sql-dashboard:<old-sha>"}]'
```

### Take a Flink savepoint manually

```bash
# Trigger savepoint (Flink Operator handles this)
kubectl annotate flinkdeployment sql-dashboard -n flink \
  flink.apache.org/savepoint-trigger-nonce=$(date +%s)

# Check savepoint path in status
kubectl get flinkdeployment sql-dashboard -n flink -o jsonpath='{.status.jobStatus.savepointInfo}'
```

### Scale node pools

```bash
# Temporarily scale up Flink nodes (e.g. for a big backlog)
az aks nodepool update \
  --cluster-name prod-eus2-orders-aks \
  --resource-group prod-eus2-orders-rg \
  --name flink \
  --min-count 3 --max-count 8
```

### Access Grafana

```bash
kubectl port-forward svc/kube-prometheus-stack-grafana 3000:80 -n monitoring
# http://localhost:3000   admin / admin123
```

### Rotate the Event Hubs connection string

1. Regenerate the SAS key in Azure Portal → Event Hubs → `prodeus2ordersehns` → Shared access policies
2. Update the Key Vault secret: `az keyvault secret set --vault-name prod-eus2-orders-kv --name eventhub-conn-string --value "<new-string>"`
3. Restart pods so the CSI driver picks up the new value:
   ```bash
   kubectl rollout restart deployment/order-generator -n orders
   kubectl rollout restart deployment/dashboard-backend -n orders
   kubectl patch flinkdeployment sql-dashboard -n flink \
     --type merge -p '{"spec":{"restartNonce":"'$(date +%s)'"}}'
   ```

---

## Known Issues & Notes

- **WSL kubeconfig**: `az aks get-credentials` writes to the Windows path. Always set
  `export KUBECONFIG=/mnt/c/Users/<user>/.kube/config` in WSL.

- **AKS K8s version**: 1.30/1.31 require LTS support plan in East US 2.
  Use 1.33 or check `az aks get-versions --location eastus2`.

- **Flink Operator OCI registry**: `ghcr.io/apache/flink-kubernetes-operator-helm-charts`
  returns HTTP 403 without authentication. Download the Helm tarball from the Apache
  archive instead (see Step 5).

- **Key Vault firewall**: `default_action = "Allow"` lets GitHub Actions runners reach
  the vault. For stricter hardening, switch to `Deny` with a self-hosted runner on a
  static IP.

- **Grafana password**: `admin123`. Change after first login via
  `Administration → Users`.
