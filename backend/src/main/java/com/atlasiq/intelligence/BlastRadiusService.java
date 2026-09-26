package com.atlasiq.intelligence;
import com.atlasiq.persistence.*; import com.atlasiq.qir.*; import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
@ApplicationScoped public class BlastRadiusService {
 private final AnalysisStore store; public BlastRadiusService(AnalysisStore store){this.store=store;}
 public BlastRadius analyze(String analysisId,String nodeId,int depth){
  QirModel m=store.find(analysisId).orElseThrow(()->new NoSuchElementException("analysis not found")).model();
  int max=Math.max(1,Math.min(depth,6)); Map<String,QirNode> nodes=new HashMap<>();m.nodes().forEach(n->nodes.put(n.id(),n));
  Set<String> visited=new LinkedHashSet<>(); Set<String> frontier=new LinkedHashSet<>(List.of(nodeId)); List<QirEdge> affectedEdges=new ArrayList<>();
  for(int d=0;d<max&&!frontier.isEmpty();d++){Set<String> next=new LinkedHashSet<>();for(QirEdge e:m.edges())if(frontier.contains(e.to())||frontier.contains(e.from())){String other=frontier.contains(e.to())?e.from():e.to();if(!visited.contains(other)){next.add(other);affectedEdges.add(e);}}visited.addAll(frontier);frontier=next;}
  visited.remove(nodeId); return new BlastRadius(analysisId,nodeId,max,visited.stream().map(nodes::get).filter(Objects::nonNull).toList(),List.copyOf(affectedEdges));
 }
 public record BlastRadius(String analysisId,String sourceNodeId,int depth,List<QirNode> affectedNodes,List<QirEdge> paths){}
}