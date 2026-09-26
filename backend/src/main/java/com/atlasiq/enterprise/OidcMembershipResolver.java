package com.atlasiq.enterprise;
import io.quarkus.security.identity.SecurityIdentity;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class OidcMembershipResolver {private final EnterpriseStore store;public OidcMembershipResolver(EnterpriseStore s){store=s;}public Set<String> roles(String org,String ws,SecurityIdentity identity){if(identity==null||identity.isAnonymous())return Set.of();return store.roles(org,ws,identity.getPrincipal().getName());}}
