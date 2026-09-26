package com.atlasiq.enterprise;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantFilterTest {

    private TenantFilter filter;
    private TenantContext tenant;

    @BeforeEach
    void setUp() {
        tenant = new TenantContext();
        filter = new TenantFilter();
        filter.context = tenant;
    }

    @Test
    void scanApiDoesNotRequireTenantHeaders() {
        ContainerRequestContext request = request("api/scans", null, null);
        assertDoesNotThrow(() -> filter.filter(request));
        assertNull(tenant.organizationId());
        assertNull(tenant.workspaceId());
    }

    @Test
    void enterpriseApiRequiresTenantHeaders() {
        ContainerRequestContext request = request("api/v1/enterprise/audit", null, null);
        assertThrows(BadRequestException.class, () -> filter.filter(request));
    }

    @Test
    void enterpriseApiPopulatesTenantContext() {
        ContainerRequestContext request = request("api/v1/enterprise/audit", "org-1", "workspace-1");
        SecurityContext security = mock(SecurityContext.class);
        Principal principal = () -> "user-1";
        when(security.getUserPrincipal()).thenReturn(principal);
        when(request.getSecurityContext()).thenReturn(security);

        filter.filter(request);

        assertEquals("org-1", tenant.organizationId());
        assertEquals("workspace-1", tenant.workspaceId());
        assertEquals("user-1", tenant.subject());
    }

    private ContainerRequestContext request(String path, String org, String workspace) {
        ContainerRequestContext request = mock(ContainerRequestContext.class);
        UriInfo uri = mock(UriInfo.class);
        when(uri.getPath()).thenReturn(path);
        when(request.getUriInfo()).thenReturn(uri);
        when(request.getHeaderString("X-AtlasIQ-Organization")).thenReturn(org);
        when(request.getHeaderString("X-AtlasIQ-Workspace")).thenReturn(workspace);
        return request;
    }
}
