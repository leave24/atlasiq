package com.atlasiq.enterprise;
import jakarta.enterprise.context.ApplicationScoped;import java.time.Instant;import java.util.*;import java.util.concurrent.CopyOnWriteArrayList;
@ApplicationScoped public class AuditLogService {
 private final List<Event> events=new CopyOnWriteArrayList<>();
 public void record(String org,String ws,String actor,String action,String resource){events.add(new Event(UUID.randomUUID().toString(),Instant.now(),org,ws,actor,action,resource));}
 public List<Event> list(String org,String ws,int limit){int n=Math.max(1,Math.min(limit,500));return events.stream().filter(e->e.organizationId().equals(org)&&e.workspaceId().equals(ws)).sorted(Comparator.comparing(Event::at).reversed()).limit(n).toList();}
 public record Event(String id,Instant at,String organizationId,String workspaceId,String actor,String action,String resource){}
}
