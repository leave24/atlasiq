# AtlasIQ Product Roadmap

This roadmap captures the next product increments after the MVP repository scanner, QIR model, architecture graph, relationship explorer, and DevSecOps findings.

The roadmap deliberately keeps architecture discovery deterministic. AI is an explanation layer over QIR and traceable evidence, not the source of architectural facts.

## Delivery order

### 1. Finish Architecture Explorer

Close the current graph iteration before adding new parser breadth.

- Highlight the selected node and its direct inbound/outbound relationships.
- Dim unrelated nodes and edges while a node is selected.
- Add reset/center controls.
- Add relationship tooltips with source, target, and relationship.
- Keep domain and edge-category filters.
- Preserve a readable large-repository experience with horizontal scrolling.

**Done when:** a user can isolate a component and understand its immediate dependencies without reading the raw relationships table.

### 2. Private GitHub repositories

Replace the public-repository-only acquisition model with GitHub App based access.

Target flow:

```text
Connect GitHub
  -> Install/authorize GitHub App
  -> Select organization/repository
  -> Select branch/ref
  -> Obtain short-lived installation token
  -> Clone/analyze
  -> Discard credentials
```

Requirements:

- Never persist installation tokens in QIR, findings, application logs, or repository URLs.
- Use least-privilege GitHub App permissions.
- Keep public repository acquisition available.
- Distinguish authentication failures from repository-not-found and network failures.
- Replace free-form repository URL input with repository/ref selection when a GitHub installation is connected.

### 3. Repository Analysis Summary

Generate a deterministic summary before the graph.

Example dimensions:

- primary languages
- detected frameworks
- build tools
- containerization
- CI/CD
- Kubernetes
- Terraform/IaC
- detected data stores where deterministically inferable
- component, relationship, and finding counts

**Done when:** a user can answer "what is in this repository?" before opening the detailed graph.

### 4. DevSecOps Findings v2

Expand findings into explicit categories.

**Security**
- privileged containers
- root/runAsNonRoot posture
- secret usage
- GitHub Actions SHA pinning
- workflow permissions
- unsafe image references

**Reliability**
- probes
- requests/limits
- replicas
- disruption/recovery signals that can be statically inferred

**CI/CD**
- action pinning
- workflow permissions
- dependency chains
- deployment protections when represented in repository configuration

**Architecture**
- orphan services
- unresolved references
- unused secrets/configuration where deterministic evidence exists
- broken dependency targets

Each finding should eventually include:

- severity
- category
- component/resource
- source file
- source line/range when available
- deterministic evidence
- recommendation

### 5. Source Traceability

Every important QIR node, relationship, and finding should be explainable from source.

Example:

```text
postgres
  reads_secret
postgres-credentials

Detected from:
k8s/postgres.yaml
spec.template.spec.containers[].envFrom[].secretRef.name
```

Requirements:

- Extend QIR provenance metadata.
- Store source path and, when parser support allows it, line/range.
- UI action from node/edge/finding to evidence.
- Do not claim line-level evidence when the parser cannot reliably provide it.

### 6. Persist analyses

Use PostgreSQL to make analysis a first-class historical object.

Suggested entities:

- Repository
- Analysis
- QIR snapshot
- Finding snapshot

Minimum metadata:

- repository identity
- ref
- commit SHA
- analysis timestamp
- parser/schema version
- node/edge/finding counts

This must preserve deterministic snapshots so later comparisons are reproducible.

### 7. Architecture Diff

Compare two persisted analyses.

Initial changes:

- added/removed components
- added/removed relationships
- changed findings
- relevant metadata changes

Example:

```text
+ Deployment payment-worker
+ Secret stripe-credentials
- Service legacy-payment
! backend replicas 2 -> 1
! privileged container introduced
```

Diffs must reference exact source/target analysis commit SHAs.

### 8. Multi-repository System View

Introduce a Workspace/System containing multiple repositories.

Goals:

- aggregate QIR from multiple repositories
- preserve repository ownership/provenance on every node
- resolve cross-repository relationships only when evidence supports them
- render repository boundaries in the architecture graph

Do not infer undocumented cross-repository dependencies merely because names look similar.

### 9. AI Architecture Assistant

Add AI only after QIR, provenance, persistence, and diffing are trustworthy.

Initial questions:

- Explain this architecture.
- Why does component A depend on component B?
- What risks were detected?
- What changed between these analyses?
- What evidence supports this finding?

Guardrails:

- Ground answers in QIR and source evidence.
- Clearly distinguish deterministic facts from AI interpretation.
- Cite source evidence in the product UI.
- Do not invent missing dependencies or infrastructure.

## Technical debt / platform work

These should be handled alongside the roadmap rather than forgotten:

- Harden Terraform parsing around module boundaries, reference resolution, and braces in strings/comments.
- Upgrade the vulnerable Next.js dependency through a controlled dependency PR and run the complete CI/build suite.
- Add E2E coverage for public repository acquisition -> scan -> QIR -> graph/findings.
- Add E2E coverage for private repository acquisition once GitHub App support exists.
- Keep credentials and secrets out of logs and persisted analysis payloads.

## Suggested implementation sequence

```text
Architecture Explorer completion
        |
        v
GitHub App / private repositories
        |
        v
Analysis Summary
        |
        v
Findings v2
        |
        v
Source Traceability
        |
        v
Persistence
        |
        v
Architecture Diff
        |
        v
Multi-repository System View
        |
        v
AI Architecture Assistant
```

Each major stage should normally be implemented in a focused follow-up PR with its own tests rather than combining all production code into one high-risk change.
