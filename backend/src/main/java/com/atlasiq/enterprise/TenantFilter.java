package com.atlasiq.enterprise;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class TenantFilter implements ContainerRequestFilter {

    private static final String ENTERPRISE_API_PREFIX = "api/v1/enterprise";

    @Inject
    TenantContext context;

    @Override
    public void filter(ContainerRequestContext request) {
        String path = request.getUriInfo().getPath();

        // Tenant headers are mandatory only for the enterprise control plane.
        // Existing scan/intelligence APIs remain backward-compatible and must
        // not fail simply because the caller has no workspace context yet.
        if (!path.startsWith(ENTERPRISE_API_PREFIX)) {
            return;
        }

        String organization = request.getHeaderString("X-AtlasIQ-Organization");
        String workspace = request.getHeaderString("X-AtlasIQ-Workspace");

        if (organization == null || organization.isBlank()
                || workspace == null || workspace.isBlank()) {
            throw new BadRequestException("organization and workspace headers are required");
        }

        SecurityContext security = request.getSecurityContext();
        String subject = security != null && security.getUserPrincipal() != null
                ? security.getUserPrincipal().getName()
                : "anonymous";

        context.set(organization.trim(), workspace.trim(), subject);
    }
}
