package com.atlasiq.intelligence;
import com.atlasiq.persistence.*;import com.atlasiq.qir.*;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class GraphLodService {
 private final AnalysisStore store;public GraphLodService(AnalysisStore store){this.store=store;}
 public ClusteredGraph graph(String id,String level){QirModel m=store.find(id).orElseThrow().model();String l=level==null?"component":level.toLowerCase();Map<String,List<QirNode>> groups=new LinkedHashMap<>();for(QirNode n:m.nodes()){String k=switch(l){case "technology"->n.technology();case "type"->n.type();case "repository"->m.repository();default->String.valueOf(n.metadata().getOrDefault("system",n.type()));};groups.computeIfAbsent(k,x->new ArrayList<>()).add(n);}List<Cluster> c=groups.entrySet().stream().map(e->new Cluster(e.getKey(),e.getValue().size(),List.copyOf(e.getValue()))).toList();return new ClusteredGraph(l,c,m.edges());}
 public record Cluster(String key,int size,List<QirNode> nodes){}public record ClusteredGraph(String level,List<Cluster> clusters,List<QirEdge> edges){}
}
