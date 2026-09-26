package com.atlasiq.enterprise;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RbacServiceTest {
    @Test
    void enforcesRolePermissions() {
        var r = new RbacService();
        assertTrue(r.allowed(Set.of("viewer"), "architecture:read"));
        assertFalse(r.allowed(Set.of("viewer"), "workspace:manage"));
        assertTrue(r.allowed(Set.of("owner"), "security:manage"));
    }
}
