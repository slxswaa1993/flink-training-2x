# Flink Deployment Guide

A comprehensive guide to deploying Apache Flink applications, from local development with Docker to production on Kubernetes.

## Table of Contents

1. [Overview](#overview)
2. [Local Development with Docker](#local-development-with-docker)
3. [Flink Deployment Modes](#flink-deployment-modes)
4. [Production Deployment on Kubernetes](#production-deployment-on-kubernetes)
5. [Kubernetes Operators Explained](#kubernetes-operators-explained)
6. [Flink Kubernetes Operator](#flink-kubernetes-operator)
7. [State Management & Fault Tolerance](#state-management--fault-tolerance)
8. [Best Practices](#best-practices)
9. [Quick Reference](#quick-reference)

---

## Overview

Apache Flink is a distributed stream processing framework. Deploying Flink involves:

1. **Cluster Setup** - JobManager (coordinator) + TaskManagers (workers)
2. **Job Submission** - Getting your application code to the cluster
3. **State Management** - Checkpoints and savepoints for fault tolerance
4. **Monitoring** - Web UI, metrics, logging

This guide covers the journey from running Flink locally with Docker to production deployments on Kubernetes.

---

## Local Development with Docker

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   Docker Environment                     │
│                                                          │
│  ┌─────────────────┐       ┌─────────────────┐          │
│  │   JobManager    │◄─────►│  TaskManager    │          │
│  │                 │       │                 │          │
│  │  - Coordinates  │       │  - Executes     │          │
│  │  - Schedules    │       │  - Processes    │          │
│  │  - Checkpoints  │       │  - State        │          │
│  │                 │       │                 │          │
│  │  Port 8081 (UI) │       │  Task Slots: N  │          │
│  └─────────────────┘       └─────────────────┘          │
│                                                          │
│  ┌─────────────────────────────────────────────────┐    │
│  │              Shared Volume: ./jars               │    │
│  │         (Your application JAR files)             │    │
│  └─────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

### Session Mode Cluster (docker-compose.yml)

A long-running cluster where you submit multiple jobs:

```yaml
services:
  jobmanager:
    image: flink:1.20-java17
    container_name: flink-jobmanager
    ports:
      - "8082:8081"  # Flink Web UI
      - "6123:6123"  # RPC port
    command: jobmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        rest.address: 0.0.0.0
    volumes:
      - ./jars:/opt/flink/usrlib
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/overview"]
      interval: 5s
      timeout: 3s
      retries: 10

  taskmanager:
    image: flink:1.20-java17
    container_name: flink-taskmanager
    depends_on:
      jobmanager:
        condition: service_healthy
    command: taskmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 4
    volumes:
      - ./jars:/opt/flink/usrlib
```

**Usage:**

```bash
# Start cluster
docker-compose up -d

# Submit jobs (multiple ways)
# 1. Via REST API
curl -X POST -F "jarfile=@jars/my-job.jar" http://localhost:8082/jars/upload

# 2. Via Flink CLI in container
docker exec flink-jobmanager flink run \
  -c com.example.MyJob /opt/flink/usrlib/my-job.jar

# 3. Via helper script (see submit-job.sh)
./submit-job.sh ride-cleansing

# View logs
docker logs -f flink-taskmanager

# Stop cluster
docker-compose down
```

### Application Mode Cluster (docker-compose.app-mode.yml)

A dedicated cluster for a single job - starts, runs the job, and can be torn down:

```yaml
services:
  jobmanager:
    image: flink:1.20-java17
    ports:
      - "8082:8081"
    command: standalone-job --job-classname ${JOB_CLASS}
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        parallelism.default: 2
    volumes:
      - ./jars/${JAR_FILE}:/opt/flink/usrlib/job.jar

  taskmanager:
    image: flink:1.20-java17
    depends_on:
      - jobmanager
    command: taskmanager
    environment:
      - |
        FLINK_PROPERTIES=
        jobmanager.rpc.address: jobmanager
        taskmanager.numberOfTaskSlots: 4
    volumes:
      - ./jars/${JAR_FILE}:/opt/flink/usrlib/job.jar
```

**Usage:**

```bash
# Run with default job
docker-compose -f docker-compose.app-mode.yml up

# Run specific job
JOB_CLASS=org.apache.flink.training.exercises.ridecleansing.RideCleansingSolution \
JAR_FILE=ride-cleansing-2.0-SNAPSHOT-all.jar \
docker-compose -f docker-compose.app-mode.yml up
```

---

## Flink Deployment Modes

### Comparison Table

| Aspect | Session Mode | Application Mode | Per-Job Mode (Deprecated) |
|--------|--------------|------------------|---------------------------|
| **Where `main()` runs** | Client | JobManager | Client |
| **Cluster lifecycle** | Long-running, shared | Per-application | Per-job |
| **Resource isolation** | Shared | Isolated | Isolated |
| **Startup overhead** | Low (cluster exists) | Higher (new cluster) | Higher |
| **Use case** | Development, multiple jobs | Production | Legacy |

### Session Mode

```
┌──────────┐     ┌─────────────────────────────────────┐
│  Client  │     │         Flink Session Cluster       │
│          │     │                                     │
│ main()   │────▶│  JobManager                         │
│ runs     │     │     │                               │
│ here     │     │     ├── Job A (your submission)     │
│          │     │     ├── Job B (another user)        │
│          │     │     └── Job C (batch job)           │
│          │     │                                     │
│          │     │  TaskManagers (shared)              │
└──────────┘     └─────────────────────────────────────┘
```

**Characteristics:**
- Client runs `main()`, generates JobGraph, submits to cluster
- Cluster resources shared among all jobs
- Good for development and interactive use
- Lower resource efficiency in production

### Application Mode

```
┌──────────┐     ┌─────────────────────────────────────┐
│  Client  │     │      Flink Application Cluster      │
│          │     │                                     │
│ triggers │────▶│  JobManager                         │
│ deploy   │     │     │                               │
│ only     │     │     └── main() runs HERE            │
│          │     │         └── Single application      │
│          │     │                                     │
│          │     │  TaskManagers (dedicated)           │
└──────────┘     └─────────────────────────────────────┘
```

**Characteristics:**
- `main()` runs on JobManager, not client
- Client only triggers deployment (can disconnect)
- One application per cluster
- Better resource isolation
- Recommended for production

### When to Use Which

| Scenario | Recommended Mode |
|----------|------------------|
| Local development | Session |
| CI/CD pipelines | Application |
| Production streaming jobs | Application |
| Ad-hoc analytics queries | Session |
| Resource-constrained environment | Session (shared resources) |
| Multi-tenant platform | Application (isolation) |

---

## Production Deployment on Kubernetes

### Why Kubernetes for Flink?

1. **Container orchestration** - Automatic scheduling, scaling, healing
2. **Resource management** - CPU/memory limits, quotas
3. **Service discovery** - DNS-based discovery for JobManager/TaskManager
4. **Rolling updates** - Zero-downtime deployments
5. **Cloud-native ecosystem** - Integrates with monitoring, logging, secrets

### Deployment Options

| Option | Complexity | Features | Use Case |
|--------|------------|----------|----------|
| **Native Kubernetes** | Medium | Basic deployment | Simple jobs, learning |
| **Flink Operator** | Low | Full lifecycle management | Production |
| **Helm Charts** | Low-Medium | Templated deployments | Quick setup |

### Native Kubernetes Deployment

Without an operator, you manage resources directly:

```yaml
# jobmanager-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: flink-jobmanager
spec:
  replicas: 1
  selector:
    matchLabels:
      app: flink
      component: jobmanager
  template:
    metadata:
      labels:
        app: flink
        component: jobmanager
    spec:
      containers:
      - name: jobmanager
        image: flink:1.20-java17
        args: ["jobmanager"]
        ports:
        - containerPort: 6123  # RPC
        - containerPort: 8081  # Web UI
        env:
        - name: FLINK_PROPERTIES
          value: |
            jobmanager.rpc.address: flink-jobmanager
            state.checkpoints.dir: s3://bucket/checkpoints
---
# jobmanager-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: flink-jobmanager
spec:
  selector:
    app: flink
    component: jobmanager
  ports:
  - name: rpc
    port: 6123
  - name: ui
    port: 8081
---
# taskmanager-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: flink-taskmanager
spec:
  replicas: 2
  selector:
    matchLabels:
      app: flink
      component: taskmanager
  template:
    metadata:
      labels:
        app: flink
        component: taskmanager
    spec:
      containers:
      - name: taskmanager
        image: flink:1.20-java17
        args: ["taskmanager"]
        env:
        - name: FLINK_PROPERTIES
          value: |
            jobmanager.rpc.address: flink-jobmanager
            taskmanager.numberOfTaskSlots: 2
```

**Drawbacks of Native Approach:**
- Manual savepoint management
- No automatic recovery logic
- Complex upgrade procedures
- No autoscaling integration

---

## Kubernetes Operators Explained

### What is an Operator?

A Kubernetes Operator is a software extension that uses **Custom Resources (CRs)** to manage applications and their components. It encodes human operational knowledge into software.

```
┌─────────────────────────────────────────────────────────────────┐
│                     Kubernetes Architecture                      │
│                                                                  │
│  ┌─────────────────┐    ┌─────────────────────────────────────┐ │
│  │   API Server    │    │           Operator                  │ │
│  │                 │    │                                     │ │
│  │  Stores CRDs &  │◄──►│  1. Watch: Monitor custom resources │ │
│  │  Custom         │    │  2. Analyze: Compare desired vs     │ │
│  │  Resources      │    │             actual state            │ │
│  │                 │    │  3. Act: Create/Update/Delete       │ │
│  └─────────────────┘    │       native K8s resources          │ │
│                         │                                     │ │
│  ┌─────────────────┐    │  Encodes:                           │ │
│  │  etcd           │    │  - How to deploy                    │ │
│  │  (state store)  │    │  - How to scale                     │ │
│  └─────────────────┘    │  - How to upgrade                   │ │
│                         │  - How to backup/restore            │ │
│                         │  - How to handle failures           │ │
│                         └─────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Operator Pattern Components

**1. Custom Resource Definition (CRD)**

Extends Kubernetes API with new resource types:

```yaml
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: flinkdeployments.flink.apache.org
spec:
  group: flink.apache.org
  names:
    kind: FlinkDeployment
    plural: flinkdeployments
    singular: flinkdeployment
    shortNames: [fd]
  scope: Namespaced
  versions:
  - name: v1beta1
    served: true
    storage: true
```

**2. Custom Resource (CR)**

An instance of the CRD - what you create:

```yaml
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment      # <-- Custom Resource
metadata:
  name: my-flink-job
spec:
  # Your desired state
```

**3. Controller**

The operator's brain - reconciliation loop:

```
┌─────────────────────────────────────────────────────────────┐
│                    Reconciliation Loop                       │
│                                                              │
│   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐ │
│   │  Watch  │───▶│ Compare │───▶│ Decide  │───▶│  Act    │ │
│   │         │    │         │    │         │    │         │ │
│   │ Monitor │    │ Desired │    │ What    │    │ Create/ │ │
│   │ events  │    │ vs      │    │ actions │    │ Update/ │ │
│   │         │    │ Actual  │    │ needed? │    │ Delete  │ │
│   └─────────┘    └─────────┘    └─────────┘    └─────────┘ │
│        ▲                                            │       │
│        └────────────────────────────────────────────┘       │
│                      (continuous)                            │
└─────────────────────────────────────────────────────────────┘
```

### Why Use Operators?

| Manual Management | With Operator |
|-------------------|---------------|
| Write many YAML files | Write one Custom Resource |
| Script upgrade procedures | Declarative upgrades |
| Manual failure recovery | Automatic healing |
| DIY monitoring integration | Built-in metrics |
| Complex CI/CD pipelines | GitOps-native |

### Popular Kubernetes Operators

| Operator | Purpose |
|----------|---------|
| Flink Operator | Stream processing |
| Prometheus Operator | Monitoring |
| Strimzi | Apache Kafka |
| PostgreSQL Operator | Databases |
| Cert-Manager | TLS certificates |
| ArgoCD | GitOps deployments |

---

## Flink Kubernetes Operator

### Overview

The [Apache Flink Kubernetes Operator](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-stable/) automates Flink deployment and lifecycle management on Kubernetes.

### Installation

```bash
# Add Helm repository
helm repo add flink-operator https://downloads.apache.org/flink/flink-kubernetes-operator-1.10.0/

# Install operator
helm install flink-kubernetes-operator flink-operator/flink-kubernetes-operator \
  --namespace flink-operator \
  --create-namespace

# Verify installation
kubectl get pods -n flink-operator
```

### Custom Resources

**FlinkDeployment** - Deploy Flink applications or session clusters:

```yaml
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment
metadata:
  name: ride-cleansing
  namespace: flink-jobs
spec:
  image: ghcr.io/your-org/ride-cleansing:v1.0.0
  flinkVersion: v1_20
  mode: Application

  flinkConfiguration:
    taskmanager.numberOfTaskSlots: "2"
    state.checkpoints.dir: s3://my-bucket/checkpoints
    state.savepoints.dir: s3://my-bucket/savepoints
    high-availability.type: kubernetes
    high-availability.storageDir: s3://my-bucket/ha

  serviceAccount: flink

  jobManager:
    resource:
      memory: "2048m"
      cpu: 1
    replicas: 1  # For HA, use 2+

  taskManager:
    replicas: 2
    resource:
      memory: "2048m"
      cpu: 1

  job:
    jarURI: local:///opt/flink/usrlib/job.jar
    entryClass: org.apache.flink.training.exercises.ridecleansing.RideCleansingSolution
    parallelism: 4
    upgradeMode: savepoint
    state: running
    savepointTriggerNonce: 0  # Increment to trigger savepoint
```

**FlinkSessionJob** - Submit jobs to session clusters:

```yaml
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment
metadata:
  name: flink-session-cluster
spec:
  flinkVersion: v1_20
  mode: Session
  jobManager:
    resource:
      memory: "2048m"
      cpu: 1
  taskManager:
    replicas: 2
    resource:
      memory: "2048m"
      cpu: 1
---
apiVersion: flink.apache.org/v1beta1
kind: FlinkSessionJob
metadata:
  name: ride-cleansing-job
spec:
  deploymentName: flink-session-cluster
  job:
    jarURI: https://my-storage.example.com/jars/ride-cleansing.jar
    entryClass: org.apache.flink.training.exercises.ridecleansing.RideCleansingSolution
    parallelism: 2
    upgradeMode: stateless
```

### Upgrade Modes

| Mode | Behavior | State Preserved | Use Case |
|------|----------|-----------------|----------|
| `stateless` | Stop and restart | No | Stateless jobs |
| `savepoint` | Savepoint → Stop → Start from savepoint | Yes | Planned upgrades |
| `last-state` | Start from last checkpoint/savepoint | Yes | Fast recovery |

### Lifecycle Management

```bash
# Deploy
kubectl apply -f flink-deployment.yaml

# Check status
kubectl get flinkdeployment
kubectl describe flinkdeployment ride-cleansing

# View pods
kubectl get pods -l app=ride-cleansing

# Access Web UI
kubectl port-forward svc/ride-cleansing-rest 8081:8081

# Trigger savepoint
kubectl patch flinkdeployment ride-cleansing --type merge \
  -p '{"spec":{"job":{"savepointTriggerNonce": 1}}}'

# Scale TaskManagers
kubectl patch flinkdeployment ride-cleansing --type merge \
  -p '{"spec":{"taskManager":{"replicas": 4}}}'

# Suspend job (takes savepoint)
kubectl patch flinkdeployment ride-cleansing --type merge \
  -p '{"spec":{"job":{"state": "suspended"}}}'

# Resume job
kubectl patch flinkdeployment ride-cleansing --type merge \
  -p '{"spec":{"job":{"state": "running"}}}'

# Delete (takes final savepoint if configured)
kubectl delete flinkdeployment ride-cleansing
```

### Autoscaling

The operator supports reactive autoscaling based on metrics:

```yaml
apiVersion: flink.apache.org/v1beta1
kind: FlinkDeployment
metadata:
  name: autoscaling-job
spec:
  # ... other config

  flinkConfiguration:
    # Enable reactive mode
    scheduler-mode: reactive
    # Autoscaler settings
    job.autoscaler.enabled: "true"
    job.autoscaler.stabilization.interval: "1m"
    job.autoscaler.metrics.window: "5m"
    job.autoscaler.target.utilization: "0.8"
    job.autoscaler.scale-down.grace-period: "10m"

  taskManager:
    replicas: 2  # Initial replicas

  # Autoscaler bounds
  job:
    # ... job config
```

---

## State Management & Fault Tolerance

### Checkpoints vs Savepoints

| Aspect | Checkpoint | Savepoint |
|--------|------------|-----------|
| **Purpose** | Fault tolerance | Operational (upgrades, migrations) |
| **Trigger** | Automatic (periodic) | Manual or operator-triggered |
| **Lifecycle** | Managed by Flink | User-managed |
| **Format** | Optimized, incremental | Portable, complete |
| **Use case** | Recovery from failures | Planned operations |

### Checkpoint Configuration

```yaml
flinkConfiguration:
  # Enable checkpointing
  execution.checkpointing.interval: "60s"
  execution.checkpointing.mode: EXACTLY_ONCE
  execution.checkpointing.min-pause: "30s"
  execution.checkpointing.timeout: "10m"
  execution.checkpointing.max-concurrent-checkpoints: "1"

  # Checkpoint storage
  state.checkpoints.dir: s3://my-bucket/checkpoints
  state.backend.type: rocksdb
  state.backend.incremental: "true"

  # Checkpoint retention
  execution.checkpointing.externalized-checkpoint-retention: RETAIN_ON_CANCELLATION
```

### High Availability

```yaml
flinkConfiguration:
  # HA configuration
  high-availability.type: kubernetes
  high-availability.storageDir: s3://my-bucket/ha

  # For multi-replica JobManager
  kubernetes.jobmanager.replicas: "2"
```

### State Backend Options

| Backend | State Size | Performance | Checkpointing |
|---------|------------|-------------|---------------|
| **HashMapStateBackend** | Small (heap) | Fast | Full snapshots |
| **EmbeddedRocksDBStateBackend** | Large (disk) | Good | Incremental |

---

## Best Practices

### Development Workflow

```
┌─────────────────────────────────────────────────────────────────┐
│                     Development Workflow                         │
│                                                                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │  Local   │    │  Build   │    │  Test    │    │  Deploy  │  │
│  │  Dev     │───▶│  JAR     │───▶│  (CI)    │───▶│  (CD)    │  │
│  │          │    │          │    │          │    │          │  │
│  │ IDE +    │    │ Gradle/  │    │ Unit +   │    │ GitOps   │  │
│  │ Docker   │    │ Maven    │    │ Integr.  │    │ or Helm  │  │
│  │ Compose  │    │          │    │ Tests    │    │          │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│                                                                  │
│  Environment: Session Mode    ──────────▶    Application Mode   │
└─────────────────────────────────────────────────────────────────┘
```

### Resource Sizing Guidelines

| Component | Memory | CPU | Notes |
|-----------|--------|-----|-------|
| **JobManager** | 1-4 GB | 0.5-2 | More for complex topologies |
| **TaskManager** | 2-8 GB | 1-4 | Depends on state size |
| **Task Slots** | 1-4 per TM | - | Usually 1 slot = 1 CPU core |

### Production Checklist

- [ ] **High Availability** - Multi-replica JobManager + HA storage
- [ ] **Checkpointing** - Enabled with external storage (S3/GCS/HDFS)
- [ ] **Monitoring** - Prometheus metrics + Grafana dashboards
- [ ] **Logging** - Centralized logging (ELK/Loki)
- [ ] **Resource Limits** - Set CPU/memory limits and requests
- [ ] **Network Policies** - Restrict pod-to-pod communication
- [ ] **Secrets Management** - Use Kubernetes secrets or external vault
- [ ] **Backup Strategy** - Regular savepoints to durable storage
- [ ] **Alerting** - Alerts for checkpoint failures, backpressure, restarts

### GitOps Deployment

```yaml
# ArgoCD Application
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: flink-jobs
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/your-org/flink-deployments.git
    targetRevision: main
    path: production
  destination:
    server: https://kubernetes.default.svc
    namespace: flink-jobs
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

---

## Quick Reference

### Docker Commands

```bash
# Session Mode
docker-compose up -d                    # Start cluster
docker-compose down                     # Stop cluster
docker logs -f flink-taskmanager        # View output

# Application Mode
docker-compose -f docker-compose.app-mode.yml up

# Job Submission
./submit-job.sh ride-cleansing          # Helper script
docker exec flink-jobmanager flink run -c <class> /path/to/jar
```

### Kubernetes Commands

```bash
# Operator
helm install flink-operator flink-operator/flink-kubernetes-operator

# Deployments
kubectl apply -f flink-deployment.yaml
kubectl get flinkdeployment
kubectl describe flinkdeployment <name>

# Operations
kubectl patch flinkdeployment <name> --type merge -p '{"spec":{"job":{"state":"suspended"}}}'
kubectl patch flinkdeployment <name> --type merge -p '{"spec":{"taskManager":{"replicas":4}}}'

# Debugging
kubectl logs -l component=jobmanager -f
kubectl port-forward svc/<name>-rest 8081:8081
```

### Flink CLI

```bash
# List jobs
flink list -m localhost:8081

# Submit job
flink run -m localhost:8081 -c <main-class> <jar-file>

# Cancel job
flink cancel -m localhost:8081 <job-id>

# Savepoint
flink savepoint -m localhost:8081 <job-id> <savepoint-path>

# Resume from savepoint
flink run -m localhost:8081 -s <savepoint-path> -c <main-class> <jar-file>
```

### REST API

```bash
# Cluster overview
curl http://localhost:8081/overview

# List jobs
curl http://localhost:8081/jobs

# Upload JAR
curl -X POST -F "jarfile=@my-job.jar" http://localhost:8081/jars/upload

# Run JAR
curl -X POST "http://localhost:8081/jars/<jar-id>/run?entry-class=<main-class>"

# Job details
curl http://localhost:8081/jobs/<job-id>

# Cancel job
curl -X PATCH "http://localhost:8081/jobs/<job-id>?mode=cancel"

# Trigger savepoint
curl -X POST -d '{"target-directory":"s3://bucket/savepoints"}' \
  http://localhost:8081/jobs/<job-id>/savepoints
```

---

## File Structure

```
flink-training-2x/
├── docker-compose.yml              # Session Mode cluster
├── docker-compose.app-mode.yml     # Application Mode cluster
├── submit-job.sh                   # Job submission helper
├── jars/                           # JAR files for submission
│   └── ride-cleansing-2.0-SNAPSHOT-all.jar
├── docs/
│   └── flink-deployment-guide.md   # This guide
└── ride-cleansing/
    └── src/main/java/...           # Application code
```

---

## Further Reading

- [Apache Flink Documentation](https://nightlies.apache.org/flink/flink-docs-stable/)
- [Flink Kubernetes Operator Docs](https://nightlies.apache.org/flink/flink-kubernetes-operator-docs-stable/)
- [Flink on Kubernetes - Native Integration](https://nightlies.apache.org/flink/flink-docs-stable/docs/deployment/resource-providers/native_kubernetes/)
- [Kubernetes Operator Pattern](https://kubernetes.io/docs/concepts/extend-kubernetes/operator/)
