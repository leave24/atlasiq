# AtlasIQ Architecture Governance

Governance is deterministic and evaluated against QIR. AI may explain a violation, but it does not decide whether a policy passed.

## Policy as code
Policy packs are versioned YAML. Initial rule primitives cover forbidden dependencies, approved/deprecated technologies and required metadata. These primitives are intended to represent:
- layer boundaries
- team boundaries
- cloud/provider restrictions
- security architecture requirements
- API-version requirements
- data-access restrictions

Example:

```yaml
version: "1"
policies:
  - id: ui-must-not-access-db
    description: UI cannot depend directly on databases
    severity: high
    rule:
      kind: forbidden-dependency
      fromType: frontend
      toType: database
      relationship: depends_on
      values: []
  - id: deprecated-runtime
    description: Deprecated runtimes are forbidden
    severity: medium
    rule:
      kind: deprecated-technologies
      values: [java8, node16]
```

## ADR as code
Markdown ADRs under `docs/adr` can declare:
- `adr-id:`
- `status:`
- `qir:` selectors such as `type:service`, `technology:kafka`, `id:<node>`
- `policies:` related policy IDs

This allows AtlasIQ to relate architectural decisions to concrete QIR nodes and later detect decision drift.

## Fitness functions
A deterministic fitness score summarizes policy conformance. High/critical violations have greater weight. The raw violations remain the source of truth; the score is a presentation aid, not a replacement for evidence.

## Next depth
Policy primitives should evolve into a schema-validated DSL with relationship direction, glob/regex selectors, ownership selectors, environments, exceptions with expiry, policy packs per workspace and PR-delta evaluation. ADR drift should compare accepted decision constraints against both desired and observed architecture.
