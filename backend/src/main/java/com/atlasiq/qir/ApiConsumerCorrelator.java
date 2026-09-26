package com.atlasiq.qir;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class ApiConsumerCorrelator {
    public List<QirEdge> correlate(List<QirNode> nodes){
        List<QirNode> clients=nodes.stream().filter(n->"http-client".equals(n.type())).toList();
        List<QirNode> apis=nodes.stream().filter(n->"api-endpoint".equals(n.type())).toList();
        List<QirEdge> out=new ArrayList<>();
        for(QirNode client:clients){
            String host=value(client,"targetHost");
            if(host==null)continue;
            List<QirNode> candidates=apis.stream().filter(api->matchesHost(api,host)).toList();
            if(candidates.size()!=1)continue;
            QirNode api=candidates.getFirst();
            Map<String,Object> evidence=new HashMap<>();
            evidence.put("confidence","explicit"); evidence.put("scope","api-consumer");
            copy(client,evidence,"evidenceFile"); copy(client,evidence,"evidenceLine"); copy(client,evidence,"evidenceKind");
            out.add(new QirEdge(client.id(),api.id(),"calls-api",Map.copyOf(evidence)));
        }
        return List.copyOf(out);
    }
    private boolean matchesHost(QirNode n,String host){
        String service=value(n,"serviceName"); String hostname=value(n,"hostname"); String h=value(n,"host");
        return host.equalsIgnoreCase(n.name())||eq(host,service)||eq(host,hostname)||eq(host,h);
    }
    private boolean eq(String a,String b){return b!=null&&a.equalsIgnoreCase(b);}
    private String value(QirNode n,String k){Object v=n.metadata()==null?null:n.metadata().get(k);return v==null?null:v.toString();}
    private void copy(QirNode n,Map<String,Object> m,String k){Object v=n.metadata()==null?null:n.metadata().get(k);if(v!=null)m.put(k,v);}
}