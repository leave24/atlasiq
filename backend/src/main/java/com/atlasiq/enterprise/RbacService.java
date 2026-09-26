package com.atlasiq.enterprise;
import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class RbacService {
 private static final Map<String,Set<String>> PERMISSIONS=Map.of("viewer",Set.of("read"),"analyst",Set.of("read","scan","export"),"admin",Set.of("read","scan","export","manage","audit","billing"),"owner",Set.of("*"));
 public boolean allowed(Set<String> roles,String permission){return roles.stream().anyMatch(r->PERMISSIONS.getOrDefault(r,Set.of()).contains("*")||PERMISSIONS.getOrDefault(r,Set.of()).contains(permission));}
}
