package com.atlasiq.enterprise;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class OrganizationServiceTest {
    @Test
    void isolatesWorkspacesByOrganization() {
        EnterpriseStore store = mock(EnterpriseStore.class);
        OrganizationService s = new OrganizationService(store);
        OrganizationService.Organization a = new OrganizationService.Organization("org-a", "A", Instant.now());
        OrganizationService.Organization b = new OrganizationService.Organization("org-b", "B", Instant.now());
        OrganizationService.Workspace wa = new OrganizationService.Workspace("ws-a", a.id(), "prod", Instant.now());
        OrganizationService.Workspace wb = new OrganizationService.Workspace("ws-b", b.id(), "prod", Instant.now());

        when(store.createOrg("A")).thenReturn(a);
        when(store.createOrg("B")).thenReturn(b);
        when(store.createWorkspace(a.id(), "prod")).thenReturn(wa);
        when(store.createWorkspace(b.id(), "prod")).thenReturn(wb);
        when(store.workspaces(a.id())).thenReturn(List.of(wa));
        when(store.workspaces(b.id())).thenReturn(List.of(wb));

        a = s.createOrg("A");
        b = s.createOrg("B");
        s.createWorkspace(a.id(), "prod");
        s.createWorkspace(b.id(), "prod");

        assertEquals(1, s.workspaces(a.id()).size());
        assertEquals(a.id(), s.workspaces(a.id()).getFirst().organizationId());
    }
}
