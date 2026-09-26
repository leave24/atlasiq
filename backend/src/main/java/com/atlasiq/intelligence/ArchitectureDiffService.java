package com.atlasiq.intelligence;
import com.atlasiq.persistence.*;
import com.atlasiq.qir.*;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class ArchitectureDiffService {
 private final AnalysisStore store;
 public ArchitectureDiffService(AnalysisStore store){this.store=store;}
 public ArchitectureDiff compare(String fromId,String toId){
  QirModel a=store.find(fromId).orElseThrow(()->new NoSuchElementException("baseline analysis not found")).model();
  QirModel b=store.find(toId).orElseThrow(()->new NoSuchElementException("target analysis not found")).model();
  Map<String,QirNode> an=index(a.nodes(),QirNode::id), bn=index(b.nodes(),QirNode::id);
  List<QirNode> added=bn.keySet().stream().filter(k->!an.containsKey(k)).sorted().map(bn::get).toList();
  List<QirNode> removed=an.keySet().stream().filter(k->!bn.containsKey(k)).sorted().map(an::get).toList();
  List<ArchitectureDiff.NodeChange> changed=an.keySet().stream().filter(bn::containsKey).filter(k->!an.get(k).equals(bn.get(k))).sorted().map(k->new ArchitectureDiff.NodeChange(an.get(k),bn.get(k))).toList();
  Map<String,QirEdge> ae=index(a.edges(),this::edgeKey), be=index(b.edges(),this::edgeKey);
  List<QirEdge> addedEdges=be.keySet().stream().filter(k->!ae.containsKey(k)).sorted().map(be::get).toList();
  List<QirEdge> removedEdges=ae.keySet().stream().filter(k->!be.containsKey(k)).sorted().map(ae::get).toList();
  Map<String,Finding> af=index(a.findings(),Finding::id), bf=index(b.findings(),Finding::id);
  List<Finding> addedFindings=bf.keySet().stream().filter(k->!af.containsKey(k)).sorted().map(bf::get).toList();
  List<Finding> resolved=af.keySet().stream().filter(k->!bf.containsKey(k)).sorted().map(af::get).toList();
  return new ArchitectureDiff(fromId,toId,added,removed,changed,addedEdges,removedEdges,addedFindings,resolved);
 }
 private <T> Map<String,T> index(List<T> values,Function<T,String> key){return values.stream().collect(Collectors.toMap(key,Function.identity(),(a,b)->a,LinkedHashMap::new));}
 private String edgeKey(QirEdge e){return e.from()+"\u0000"+e.relationship()+"\u0000"+e.to()+"\u0000"+String.valueOf(e.metadata());}
}
