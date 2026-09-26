package com.atlasiq.intelligence;
import com.atlasiq.persistence.*;import com.atlasiq.qir.*;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class SecurityGraphService {
 private final AnalysisStore store;public SecurityGraphService(AnalysisStore store){this.store=store;}
 public SecurityGraph graph(String id){QirModel m=store.find(id).orElseThrow(()->new NoSuchElementException("analysis not found")).model();Set<String> ids=new LinkedHashSet<>();
  for(Finding f:m.findings())if("security".equalsIgnoreCase(f.category()))ids.add(f.resourceId());
  for(QirEdge e:m.edges())if(Set.of("reads_secret","mounts_secret","uses_service_account").contains(e.relationship())){ids.add(e.from());ids.add(e.to());}
  List<QirNode> nodes=m.nodes().stream().filter(n->ids.contains(n.id())).toList();List<QirEdge> edges=m.edges().stream().filter(e->ids.contains(e.from())&&ids.contains(e.to())).toList();
  List<Finding> findings=m.findings().stream().filter(f->ids.contains(f.resourceId())).toList();return new SecurityGraph(id,nodes,edges,findings);}
 public record SecurityGraph(String analysisId,List<QirNode> nodes,List<QirEdge> edges,List<Finding> findings){}
}