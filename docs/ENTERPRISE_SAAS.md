# AtlasIQ Enterprise / SaaS foundation

## Identity
OIDC is the runtime authentication foundation and can integrate with enterprise identity providers. SAML is isolated behind the same SSO provider contract so an enterprise SAML adapter can be activated without coupling QIR or business services to an IdP.

## Authorization
RBAC defines viewer, analyst, admin and owner permission sets. Authentication and authorization are separate concerns.

## Organizations, workspaces and tenancy
Every enterprise request carries explicit organization/workspace context. Persistent resources must include both tenant identifiers. Tenant identifiers must come from trusted authenticated membership in production; headers are an integration boundary, not a trust mechanism.

## Audit
Security-relevant reads and enterprise mutations emit structured audit events with actor, organization, workspace, action, resource and timestamp.

## Public API
Versioned routes live under `/api/v1` to preserve future API compatibility.

## Webhooks
Webhook registrations require HTTPS and explicit subscribed event types. Production delivery should add HMAC signatures, delivery IDs, retry/backoff, idempotency and SSRF-safe destination validation.

## Billing / licensing
Billing is represented by an internal subscription contract (free/team/enterprise, seats, status). A payment provider such as Stripe or an enterprise license server should be implemented as an adapter, not embedded into AtlasIQ domain logic.

## Production hardening
The current PR establishes boundaries and APIs. Before Internet-facing SaaS deployment: persist organizations/memberships/RBAC/audit/webhooks/subscriptions, derive tenant context from verified identity claims, enforce RBAC at endpoints, add webhook delivery workers/signatures, API keys/service accounts/rate limits, SAML library integration, and payment-provider webhook verification.
