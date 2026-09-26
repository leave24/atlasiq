package com.atlasiq.enterprise;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class RbacServiceTest{@Test void enforcesRolePermissions(){var r=new RbacService();assertTrue(r.allowed(Set.of("viewer"),"read"));assertFalse(r.allowed(Set.of("viewer"),"manage"));assertTrue(r.allowed(Set.of("owner"),"billing"));}}
