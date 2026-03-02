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

## CI/CD Workflows

| Workflow | Trigger | Actions |
|---|---|---|
| `pr-checks.yml` | Pull request | Python lint, Next.js build, `terraform plan` |
| `build-and-push.yml` | Push to `main` | Build 4 Docker images → ACR |
| `deploy-app.yml` | After build | `kubectl apply -k` + patch FlinkDeployment image |
| `deploy-infrastructure.yml` | Manual / infra file change | `terraform apply` |

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
