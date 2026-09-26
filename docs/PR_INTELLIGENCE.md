# AtlasIQ PR Intelligence

PR Intelligence scans the exact base SHA and head SHA of a proposed change, persists both QIR snapshots, computes Architecture Diff and PR Change Impact, and derives security, dependency, API/database breaking-change, policy and blast-radius signals.

## Providers
The core is provider-neutral through `PrProvider`.
- GitHub: pull request resolution, Checks API publishing and automatic PR comments are implemented.
- GitLab: MR adapter boundary is registered; API configuration/implementation remains required.
- Bitbucket: PR adapter boundary is registered; API configuration/implementation remains required.

Provider tokens are request-scoped and must not be persisted or logged.

## Flow
provider PR/MR -> resolve base/head SHA -> scan both revisions -> persist QIR snapshots -> diff -> security/dependency/breaking deltas -> policy evaluation -> blast radius -> provider check/comment.

## Breaking changes
Current deterministic signals flag removed `api-endpoint` and `database-object` nodes. Future schema-aware compatibility rules should distinguish compatible additions from signature/type/nullability changes.

## Policies
The initial engine demonstrates deterministic architecture policies. Policy-as-code should evolve into versioned workspace policy packs.

## Safety
Publishing is an explicit endpoint separate from analysis. GitHub check failure currently occurs for critical risk or any architecture policy violation; teams should make that gate configurable before enforcing it as a required branch protection check.
