# AtlasIQ

Temporary project name for an architecture intelligence SaaS that reconstructs software, infrastructure and CI/CD architecture from source repositories.

## MVP principles

- Local-first development.
- Kubernetes as the runtime platform.
- KIND as the official local cluster.
- Cloud-agnostic core.
- Deterministic parsing before AI-assisted explanation.

## Local architecture

The initial stack runs entirely on a local KIND cluster:

- `atlasiq-frontend`: Next.js web UI.
- `atlasiq-backend`: Quarkus Java 21 API and analysis engine.
- `postgres`: local persistence foundation.
- `ingress-nginx`: routes `atlasiq.local` traffic to frontend and API.

The KIND cluster contains one control-plane node and two worker nodes. Host ports `8080` and `8443` are mapped to the control-plane for local HTTP/HTTPS access.

## Prerequisites

Use WSL2/Linux with:

- Docker
- kubectl
- KIND
- Helm
- GNU Make
- curl

## Start the full local stack

```bash
make up
```

This creates the cluster, installs ingress-nginx, builds the backend/frontend images, loads them into KIND, deploys Kubernetes resources and runs a smoke test.

Inspect the environment with:

```bash
make status
```

Add this host entry if you want to browse by hostname:

```text
127.0.0.1 atlasiq.local
```

Then open:

```text
http://atlasiq.local:8080
```

Destroy everything with:

```bash
make destroy
```

## Current MVP flow

```text
Repository
   |
   v
Scanner
   |
   v
Deterministic Parsers
   |
   v
QIR
   |
   +--> Dependency Resolver
   +--> DevSecOps Rules
   +--> Architecture Graph
```

Current platform capabilities include deterministic Kubernetes/Docker/Terraform/GitHub Actions parsing, multi-repository QIR, dependency/API/database/supply-chain discovery, persisted architecture history, diff/PR impact, blast radius, drift, multi-cloud and contract discovery, service catalog/search, Time Machine, exports, grounded Copilot foundations, and enterprise identity/tenancy boundaries.\n\nCurrent engineering priority: **Core Hardening** — QIR v2 identity/provenance, parser SPI, Flyway migrations, end-to-end quality gates, observability, bounded/asynchronous scans, and frontend modularization. See `docs/CORE_HARDENING.md`.
