# P4 — Architecture Digital Twin

AtlasIQ evolves from reconstructing architecture into an evidence-first temporal digital twin.

## Architecture Simulation Engine
`POST /api/twin/{analysis}/simulate` applies counterfactual removals to a copy of the architecture topology and computes deterministic downstream impact, evidence paths, affected component types and risk. It never mutates the persisted QIR.

The first scenario primitive is component removal. Replacement/migration scenarios should be represented as explicit graph transformations rather than LLM guesses.

## Architecture Truth / confidence
`GET /api/twin/{analysis}/truth` evaluates evidence coverage for each component. States are deliberately epistemic rather than health labels: INFERRED, DECLARED, OBSERVED, VERIFIED and STALE.

The initial confidence decay uses evidence age. Future QIR evidence migration should make timestamps/source kinds first-class instead of relying on compatibility metadata.

## Architecture Black Box
`GET /api/twin/black-box` reconstructs architecture transitions across persisted snapshots in an incident window. This creates the foundation for correlating deployments, incidents and runtime telemetry without pretending correlation is causation.

## Architecture Memory
`GET /api/twin/memory` returns when a stable QIR component first appeared and when its representation changed across repository history.

## Hotspot Radar
`GET /api/twin/{analysis}/hotspots` combines graph centrality proxy (degree) and findings. Git churn, incident frequency and runtime criticality are explicit next evidence dimensions.

## Architecture Change Budget
`POST /api/twin/budget` supports deterministic limits for new dependencies, new public APIs and blast radius. This is intended to complement policy-as-code with measurable architecture-change budgets.

## ADR suggestions
`GET /api/twin/adr-suggestions` detects architecture changes significant enough to warrant a decision record and returns a draft context + QIR references. Humans still own architecture decisions.

## Agent Guard
`POST /api/twin/{analysis}/agent-guard` provides a machine-consumable preflight boundary for coding agents. It returns SAFE or REQUIRES_REVIEW based on deterministic simulation evidence; it does not grant permissions or autonomously approve consequential changes.

A future MCP adapter should expose read-only QIR context, simulation, policies, ADRs and Agent Guard to external coding agents.

## Design principle
AtlasIQ must distinguish facts, observations and inference. Missing runtime evidence does not mean a dependency does not exist. AI can explain evidence but must not manufacture graph facts.
