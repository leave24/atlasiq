package com.atlasiq.enterprise;
import jakarta.enterprise.context.ApplicationScoped;import java.util.*;import java.util.concurrent.ConcurrentHashMap;
@ApplicationScoped public class BillingService {
 private final Map<String,Subscription> subscriptions=new ConcurrentHashMap<>();
 public Subscription assign(String org,String plan,int seats){if(seats<1)throw new IllegalArgumentException("seats must be positive");String p=Set.of("free","team","enterprise").contains(plan)?plan:"free";var s=new Subscription(org,p,seats,status(p));subscriptions.put(org,s);return s;}
 public Subscription get(String org){return subscriptions.getOrDefault(org,new Subscription(org,"free",1,"active"));}
 private String status(String p){return "active";}public record Subscription(String organizationId,String plan,int seats,String status){}
}
