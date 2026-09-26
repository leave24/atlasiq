package com.atlasiq.enterprise;
import jakarta.enterprise.context.ApplicationScoped;import java.time.Instant;import java.util.*;import java.util.concurrent.ConcurrentHashMap;
@ApplicationScoped public class OrganizationService {
 private final Map<String,Organization> orgs=new ConcurrentHashMap<>();private final Map<String,Workspace> workspaces=new ConcurrentHashMap<>();
 public Organization createOrg(String name){String id=UUID.randomUUID().toString();var x=new Organization(id,name,Instant.now());orgs.put(id,x);return x;}
 public Workspace createWorkspace(String org,String name){if(!orgs.containsKey(org))throw new NoSuchElementException("organization not found");String id=UUID.randomUUID().toString();var x=new Workspace(id,org,name,Instant.now());workspaces.put(id,x);return x;}
 public List<Workspace> workspaces(String org){return workspaces.values().stream().filter(w->w.organizationId().equals(org)).toList();}
 public record Organization(String id,String name,Instant createdAt){}public record Workspace(String id,String organizationId,String name,Instant createdAt){}
}
