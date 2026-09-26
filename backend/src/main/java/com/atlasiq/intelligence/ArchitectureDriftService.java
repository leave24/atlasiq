package com.atlasiq.intelligence;
import jakarta.enterprise.context.ApplicationScoped;
@ApplicationScoped public class ArchitectureDriftService {
 private final ArchitectureDiffService diffs; public ArchitectureDriftService(ArchitectureDiffService diffs){this.diffs=diffs;}
 public Drift detect(String desiredAnalysisId,String observedAnalysisId){
  ArchitectureDiff d=diffs.compare(desiredAnalysisId,observedAnalysisId);
  int score=Math.min(100,d.removedNodes().size()*15+d.addedNodes().size()*8+d.changedNodes().size()*10+d.removedEdges().size()*8+d.addedEdges().size()*4);
  String level=score>=70?"critical":score>=40?"high":score>=15?"medium":"low";
  return new Drift(desiredAnalysisId,observedAnalysisId,score,level,d);
 }
 public record Drift(String desiredAnalysisId,String observedAnalysisId,int score,String level,ArchitectureDiff diff){}
}