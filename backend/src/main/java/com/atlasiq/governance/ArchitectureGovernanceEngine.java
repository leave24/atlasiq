package com.atlasiq.governance;
import com.atlasiq.qir.*;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class ArchitectureGovernanceEngine {
 public List<PolicyViolation> evaluate(QirModel m,PolicyPack pack){List<PolicyViolation> out=new ArrayList<>();Map<String,QirNode> nodes=new HashMap<>();m.nodes().forEach(n->nodes.put(n.id(),n));for(ArchitecturePolicy p:pack.policies()){var r=p.rule();switch(r.kind()){
 case "forbidden-dependency"->{for(QirEdge e:m.edges()){QirNode a=nodes.get(e.from()),b=nodes.get(e.to());if(a!=null&&b!=null&&match(a.type(),r.fromType())&&match(b.type(),r.toType())&&match(e.relationship(),r.relationship()))out.add(v(p,e.from(),"Forbidden dependency "+e.from()+" -> "+e.to(),List.of(e.from(),e.to())));}}
 case "approved-technologies"->{for(QirNode n:m.nodes())if(!r.values().isEmpty()&&!n.technology().isBlank()&&!r.values().contains(n.technology()))out.add(v(p,n.id(),"Technology is not approved: "+n.technology(),List.of(n.id())));}
 case "deprecated-technologies"->{for(QirNode n:m.nodes())if(r.values().contains(n.technology()))out.add(v(p,n.id(),"Deprecated technology detected: "+n.technology(),List.of(n.id())));}
 case "metadata"->{for(QirNode n:m.nodes())if(match(n.type(),r.fromType())&&!Objects.equals(String.valueOf(n.metadata().get(r.metadataKey())),r.metadataValue()))out.add(v(p,n.id(),"Required metadata "+r.metadataKey()+"="+r.metadataValue(),List.of(n.id())));}
 default->{}
 }}return List.copyOf(out);}
 private boolean match(String actual,String expected){return expected==null||expected.isBlank()||"*".equals(expected)||Objects.equals(actual,expected);}
 private PolicyViolation v(ArchitecturePolicy p,String id,String m,List<String> e){return new PolicyViolation(p.id(),p.severity(),id,m,e);}
}