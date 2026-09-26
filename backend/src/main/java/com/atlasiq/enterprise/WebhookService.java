package com.atlasiq.enterprise;
import jakarta.enterprise.context.ApplicationScoped;import java.net.URI;import java.util.*;import java.util.concurrent.ConcurrentHashMap;
@ApplicationScoped public class WebhookService {
 private final Map<String,Webhook> hooks=new ConcurrentHashMap<>();
 public Webhook register(String org,String ws,String url,Set<String> events){URI u=URI.create(url);if(!"https".equalsIgnoreCase(u.getScheme()))throw new IllegalArgumentException("webhook URL must use HTTPS");String id=UUID.randomUUID().toString();var h=new Webhook(id,org,ws,u.toString(),Set.copyOf(events),true);hooks.put(id,h);return h;}
 public List<Webhook> list(String org,String ws){return hooks.values().stream().filter(h->h.organizationId().equals(org)&&h.workspaceId().equals(ws)).toList();}
 public record Webhook(String id,String organizationId,String workspaceId,String url,Set<String> events,boolean enabled){}
}
