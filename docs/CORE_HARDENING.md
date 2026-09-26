# AtlasIQ Core Hardening

## QIR v2
QIR v2 formalizes stable identity, canonical metadata and provenance. Migration is incremental so existing parsers remain compatible while each contributor adopts evidence objects and stable IDs.

## Parser SPI
`RepositoryAnalysisContributor` and `RepositoryAnalysisRegistry` establish the extension boundary. Existing parsers can be migrated contributor-by-contributor instead of continuing to grow `DefaultRepositoryScanner` constructor injection.

## Persistence
Schema creation belongs to Flyway. Runtime Java code no longer owns DDL. The baseline schema adds indexes for history and future tenant-scoped access.

## Observability
SmallRye Health, Prometheus/Micrometer and OpenTelemetry support are included. OTEL is disabled by default until an exporter is configured.

## Scan safety
Central configuration establishes maximum file count, file size and scan timeout budgets. Parser migration must consume these limits consistently. Long-running analysis should move to the asynchronous job contract rather than extending HTTP request duration.

## Testing gates
Core flows requiring E2E coverage are: repository scan/persist/read, multi-repository aggregation, Architecture Diff, Blast Radius and PR Impact. These are product gates, not optional parser unit tests.

## Frontend
A reusable UI primitive module starts the design-system extraction. The existing monolithic page should be migrated incrementally into scan form, summary, graph, relationships and findings components to minimize visual regressions.
