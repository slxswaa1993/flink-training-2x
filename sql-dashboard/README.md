# SQL Dashboard — Production Guide

This module contains two things in the same directory:

| | Class | Purpose |
|---|---|---|
| **Exercise** | `SqlDashboardExercise` | In-memory DataGen source, prints to console — for learning |
| **Production** | `SqlDashboardJob` (in `production/`) | Reads from Azure Event Hubs, writes 4 analytics topics |

This document covers the **production job only**.

---

## What the job does

`SqlDashboardJob` runs 4 continuous Flink SQL queries simultaneously:

```
Azure Event Hubs
  orders-raw  ──────────────────────────────────────────────────┐
                                                                 │
                                                         Flink SQL Job
                                                                 │
              ┌─ TUMBLE(1 min)  ──→  orders-by-region           │
              ├─ HOP(30s / 1m)  ──→  top-products               │
              ├─ TUMBLE(1 min)  ──→  revenue-by-category        │
              └─ FILTER >$500   ──→  high-value-alerts           │
                                                                 └─ (all 4 are sinks)
```

| Query | Window | Output topic | What it computes |
|---|---|---|---|
| Orders by region | TUMBLE 1 min | `orders-by-region` | order count + revenue per region per minute |
| Top products | HOP 30s/1min | `top-products` | top-3 products by quantity sold (sliding) |
| Revenue by category | TUMBLE 1 min | `revenue-by-category` | revenue, avg order value, unique customers |
| High-value alerts | none (filter) | `high-value-alerts` | every order with `totalAmount > $500` |

---

## Azure resources used

### Azure Event Hubs (the message bus)

| Property | Value |
|---|---|
| Namespace | `prodeus2ordersehns` |
| Kafka bootstrap endpoint | `prodeus2ordersehns.servicebus.windows.net:9093` |
| Auth protocol | SASL_SSL with `$ConnectionString` username |
| SKU | Premium (supports Kafka protocol natively) |

**Topics the job touches:**

| Topic | Role | Partitions |
|---|---|---|
| `orders-raw` | **Source** — job reads from here | 4 |
| `orders-by-region` | **Sink** — job writes here | 4 |
| `top-products` | **Sink** — job writes here | 4 |
| `revenue-by-category` | **Sink** — job writes here | 4 |
| `high-value-alerts` | **Sink** — job writes here | 4 |

### Azure Blob Storage (checkpoint + savepoint storage)

| Property | Value |
|---|---|
| Account | `prodeus2ordersstorage` |
| Checkpoint path | `abfs://flink-state@prodeus2ordersstorage.dfs.core.windows.net/checkpoints` |
| Savepoint path | `abfs://flink-state@prodeus2ordersstorage.dfs.core.windows.net/savepoints` |

Flink writes a checkpoint every 60 seconds. If the job crashes, it restarts from the
latest checkpoint with no data loss (EXACTLY_ONCE mode).

### Azure Key Vault (secret storage)

| Property | Value |
|---|---|
| Vault | `prod-eus2-orders-kv` |
| Secret: `eventhub-conn-string` | SAS connection string injected as `EVENTHUB_CONN_STRING` |
| Secret: `eventhub-namespace` | `prodeus2ordersehns` injected as `EVENTHUB_NAMESPACE` |

The Flink pod **never has secrets in its YAML**. The Key Vault CSI driver mounts them
as a Kubernetes Secret (`eventhub-secrets`) which is then projected as env vars.

---

## How the connection is wired

```
Key Vault
  eventhub-conn-string ──┐
  eventhub-namespace   ──┤
                         │  (CSI driver mounts as K8s Secret)
                         ▼
              Secret: eventhub-secrets
                         │
                         │  (env vars injected into pod)
                         ▼
              Flink pod env:
                EVENTHUB_NAMESPACE   = "prodeus2ordersehns"
                EVENTHUB_CONN_STRING = "Endpoint=sb://..."
                         │
                         │  (JobConfig reads env vars at startup)
                         ▼
              KafkaTableFactory builds DDL:
                bootstrap.servers = prodeus2ordersehns.servicebus.windows.net:9093
                security.protocol = SASL_SSL
                sasl.mechanism    = PLAIN
                sasl.jaas.config  = PlainLoginModule username="$ConnectionString"
                                                     password="<conn-string>"
                         │
                         ▼
              Flink SQL Tables  ──→  Event Hubs topics
```

### Key files

| File | Role |
|---|---|
| `production/JobConfig.java` | Reads `EVENTHUB_NAMESPACE` and `EVENTHUB_CONN_STRING` env vars; builds `bootstrapServers()` and `saslJaasConfig()` |
| `production/KafkaTableFactory.java` | Builds the `CREATE TABLE` DDL for source (`orders-raw`) and 4 sink topics |
| `production/SqlDashboardJob.java` | Main class: creates tables, runs 4 `INSERT INTO` SQL statements |
| `k8s/base/orders/secret-provider-class.yaml` | Key Vault CSI `SecretProviderClass` — syncs KV secrets → K8s Secret |
| `k8s/base/flink/sql-dashboard-flinkdeployment.yaml` | `FlinkDeployment` CR — tells the Flink Operator how to run the job |
| `infra/docker/flink-job/Dockerfile` | Multi-stage build: Gradle → fat JAR → Flink base image |

---

## Environment variables

| Variable | Required | Source in prod | Example |
|---|---|---|---|
| `EVENTHUB_NAMESPACE` | Yes | Key Vault secret `eventhub-namespace` | `prodeus2ordersehns` |
| `EVENTHUB_CONN_STRING` | Yes | Key Vault secret `eventhub-conn-string` | `Endpoint=sb://prodeus2ordersehns...` |
| `EVENTHUB_CONSUMER_GROUP` | No | hardcoded default | `$Default` |
| `FLINK_CHECKPOINT_DIR` | No | hardcoded default | `abfs://flink-state@...` |

---

## Building

```bash
# From the project root — produces sql-dashboard/build/libs/sql-dashboard-*-production.jar
./gradlew :sql-dashboard:shadowJar

# The fat JAR includes Kafka connector and all Flink runtime deps
# Main class: org.apache.flink.training.exercises.sqldashboard.production.SqlDashboardJob
```

The `shadowJar` task (configured in `build.gradle`) sets the production main class and
appends `-production` to the classifier so it's distinct from the exercise JAR.

---

## Deploying to AKS

### Pre-requisites
- AKS cluster running with Flink Kubernetes Operator installed in `flink-operator` namespace
- Docker image pushed to `prodeus2ordersacr.azurecr.io/flink-sql-dashboard:latest`
- Key Vault CSI driver enabled on AKS (it is — set in Terraform via `key_vault_secrets_provider`)

### Step 1 — Build and push the Docker image

The CI/CD pipeline does this automatically on every push to `main`. To do it manually:

```bash
# From the project root
az acr login --name prodeus2ordersacr

docker build \
  -f infra/docker/flink-job/Dockerfile \
  -t prodeus2ordersacr.azurecr.io/flink-sql-dashboard:latest \
  .

docker push prodeus2ordersacr.azurecr.io/flink-sql-dashboard:latest
```

### Step 2 — Apply the SecretProviderClass

This tells the CSI driver to pull secrets from Key Vault and create a K8s Secret:

```bash
# Fill in the tenant ID first (one-time edit)
TENANT_ID=$(az account show --query tenantId -o tsv)
sed -i "s/tenantId: \"\"/tenantId: \"$TENANT_ID\"/" k8s/base/orders/secret-provider-class.yaml

kubectl apply -f k8s/base/orders/secret-provider-class.yaml -n flink
```

### Step 3 — Apply RBAC for the Flink service account

```bash
kubectl apply -f k8s/base/flink/rbac.yaml
```

### Step 4 — Deploy the FlinkDeployment

```bash
# Patch the image placeholder with the real ACR address
sed 's|REGISTRY|prodeus2ordersacr.azurecr.io|' \
  k8s/base/flink/sql-dashboard-flinkdeployment.yaml \
  | kubectl apply -f - -n flink
```

Or use the Kustomize production overlay (which rewrites the image automatically):

```bash
kubectl apply -k k8s/overlays/production
```

### Step 5 — Verify

```bash
# Watch the FlinkDeployment status
kubectl get flinkdeployment sql-dashboard -n flink -w

# Expected lifecycle:
#   DEPLOYING → DEPLOYED → STABLE (takes ~60–90 seconds)

# Check the JobManager and TaskManager pods
kubectl get pods -n flink

# Tail the JobManager logs
kubectl logs -n flink -l component=jobmanager -f

# Confirm data is flowing into Event Hubs
# (Azure Portal → prodeus2ordersehns → orders-by-region → Metrics → Incoming Messages)
```

---

## Upgrading the job

The `FlinkDeployment` uses `upgradeMode: savepoint`. When you push a new image:

1. The Flink Operator triggers a savepoint on the running job
2. The old pods are terminated
3. New pods start and restore from the savepoint
4. No data is lost and the job resumes from exactly where it left off

To trigger manually:
```bash
# Update the image tag (CI/CD does this automatically)
kubectl set image deployment/... # not applicable for FlinkDeployment

# Instead, edit the FlinkDeployment image field:
kubectl patch flinkdeployment sql-dashboard -n flink \
  --type merge \
  -p '{"spec":{"image":"prodeus2ordersacr.azurecr.io/flink-sql-dashboard:<new-sha>"}}'
```

---

## Troubleshooting

| Symptom | Where to look |
|---|---|
| Job stays in `DEPLOYING` | `kubectl describe flinkdeployment sql-dashboard -n flink` |
| Pod `CrashLoopBackOff` | `kubectl logs -n flink <pod-name> --previous` |
| `EVENTHUB_NAMESPACE not set` | SecretProviderClass not applied, or CSI driver not mounting |
| `Authentication failed` (Kafka) | Wrong connection string in Key Vault secret `eventhub-conn-string` |
| `ForbiddenByFirewall` (KV) | Key Vault network ACL blocking — check `default_action` in `keyvault.tf` |
| Checkpoint failures | Check `prodeus2ordersstorage` container permissions; AKS kubelet identity needs Storage Blob Data Contributor |
| No data in output topics | Check `orders-raw` is receiving messages (order-generator running?) |
