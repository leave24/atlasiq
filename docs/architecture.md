# AtlasIQ MVP Architecture

## Goal

Turn repository configuration into a deterministic architecture model before any LLM is involved.

## Initial flow

GitHub repository -> scanner -> specialized parsers -> QIR -> dependency resolver -> DevSecOps rules -> API/UI.

## MVP parsers

- Docker
- Kubernetes
- Terraform
- GitHub Actions

## QIR v1

QIR contains nodes, edges and findings. Parsers emit normalized resources; resolvers connect them; renderers and AI consume QIR rather than raw repository files.

## Principles

1. Deterministic extraction first.
2. AI only for explanation/recommendation over normalized metadata.
3. Modular monolith before microservices.
4. Least-privilege GitHub integration.
5. Enterprise path: private runner/agent that can keep source code inside the customer environment.
