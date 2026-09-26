package com.atlasiq.intelligence;
import com.atlasiq.qir.*;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class RuntimeKubernetesCorrelator {
 public List<QirEdge> correlate(List<QirNode> nodes){List<QirNode> runtime=nodes.stream().filter(n->"kubernetes-runtime-workload".equals(n.type())).toList();List<QirNode> declared=nodes.stream().filter(n->n.type().startsWith("kubernetes-")&&!"kubernetes-runtime-workload".equals(n.type())).toList();List<QirEdge> out=new ArrayList<>();
  for(QirNode r:runtime){Object ns=r.metadata().get("namespace");for(QirNode d:declared)if(r.name().equals(d.name())&&Objects.equals(ns,d.metadata().get("namespace")))out.add(new QirEdge(d.id(),r.id(),"observed_as",Map.of("confidence","explicit","scope","runtime")));}
  return List.copyOf(out);}
}