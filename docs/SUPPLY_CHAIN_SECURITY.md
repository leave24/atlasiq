# AtlasIQ Supply Chain & Security

## SBOM
AtlasIQ exports persisted software-package QIR nodes as CycloneDX 1.6 JSON and SPDX 2.3 JSON documents. Package identity uses Package URL style coordinates where sufficient version data exists.

## Dependency resolution
The existing manifest inventory is complemented by a package-lock parser that records resolved npm packages and transitive dependency edges. Additional lockfile ecosystems should use the same QIR relationship model.

## Vulnerability intelligence
`VulnerabilitySource` decouples vulnerability feeds from the graph. OSV querying is implemented for resolved package versions. A GitHub Advisory Database adapter boundary exists but authenticated advisory ingestion is not yet implemented and must not be represented as complete.

## Container security
Dockerfiles produce explicit base-image nodes marked for vulnerability scanning. Full package inventories and base-image CVEs require an OCI image scanner/registry integration (for example Trivy/Grype-compatible ingestion) and are intentionally not fabricated from Dockerfile text.

## Correlation
Security posture traverses deterministic QIR relationships to correlate vulnerable packages with service/component/repository, runtime/Kubernetes nodes and team/owner nodes when those relationships exist.

## Secret and IaC scanning
Initial deterministic scanners detect common credential patterns, AWS access key identifiers/private keys, public Terraform CIDRs, privileged Kubernetes containers and root workloads. Production secret scanning should add entropy/allowlists and provider validation to reduce false positives.

## Security blast radius
Security traversal follows an allowlist of dependency, deployment, ownership and secret relationships rather than arbitrary graph adjacency.

## Posture
The API exposes SBOM documents, vulnerability posture and security blast radius. Posture scores summarize evidence; findings/vulnerabilities and graph paths remain authoritative.

## APIs
- `GET /api/security/{analysis}/sbom/cyclonedx`
- `GET /api/security/{analysis}/sbom/spdx`
- `GET /api/security/{analysis}/posture`
- `GET /api/security/{analysis}/blast-radius/{resource}?depth=3`
