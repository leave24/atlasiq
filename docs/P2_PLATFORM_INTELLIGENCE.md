# AtlasIQ P2 Platform Intelligence

This layer extends QIR without making the core depend on a cloud vendor or an LLM.

## Discovery
- AWS, Azure and GCP resources are discovered from explicit Terraform resource declarations.
- OpenAPI/Swagger documents become API contract and endpoint nodes.
- AsyncAPI documents become event contracts/channels; Kafka and RabbitMQ/AMQP are identified from explicit protocol evidence.
- CODEOWNERS and catalog/ownership descriptors become team ownership evidence.

## Service catalog and search
The catalog is a projection of persisted QIR. Global component search operates over node id, name, type and technology and is bounded to 200 results.

## Copilot
The initial Copilot is retrieval-only and grounded in the selected persisted analysis. It returns the QIR evidence used for its answer and marks `generative=false`. A future LLM provider can synthesize natural language only after retrieval, preserving evidence as the source of truth.

## Time Machine
Time Machine uses persisted analysis history and Architecture Diff to reconstruct transitions between snapshots.

## Exports
Server-side exports support Mermaid, PlantUML, SVG, PNG and PDF. Diagram source formats preserve relationships; raster/document exports provide a portable architecture inventory view.

## LOD
Graph clustering supports repository, technology, type and component/system-oriented grouping. This is an API foundation for an interactive frontend graph.
