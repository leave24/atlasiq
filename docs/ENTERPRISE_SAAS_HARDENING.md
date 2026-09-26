## AtlasIQ Enterprise SaaS hardening

This iteration moves organization/workspace/membership metadata and audit/usage state from process memory into relational persistence.

Implemented foundations include persisted organizations/workspaces/memberships, tenant-aware schema columns and indexes, fine-grained permission names, API key hashing/one-time secret issuance, service-account persistence, tamper-evident hash-chained audit events, usage metering with idempotency keys, plan feature/quota definitions, webhook HMAC signing and SSRF destination validation, OIDC subject-to-membership lookup, SCIM membership provisioning service, Stripe configuration boundary, and RSA-signed on-prem license verification.

Important production boundaries: database-vendor row-level security policies are not portable across PostgreSQL/MySQL/SQL Server and therefore are not falsely claimed here; application queries must always include tenant predicates and PostgreSQL deployments can add native RLS in a PostgreSQL-specific migration. Full SAML requires XML signature validation and IdP metadata. Stripe needs provider API calls/webhook reconciliation. Webhook delivery still needs a worker around the persisted retry table. Rate limiting should be backed by a distributed store for multi-instance SaaS.

Tenant isolation tests must cover cross-organization and cross-workspace reads for every tenant-scoped repository. Tenant identity should come from authenticated membership, not trusted client headers, before public multi-tenant exposure.
