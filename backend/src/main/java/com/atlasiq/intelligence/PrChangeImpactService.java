package com.atlasiq.intelligence;
import com.atlasiq.qir.*;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;

@ApplicationScoped
public class PrChangeImpactService {
 private final ArchitectureDiffService diffs;
 public PrChangeImpactService(ArchitectureDiffService diffs){this.diffs=diffs;}
 public PrChangeImpact assess(String baseline,String candidate){
  ArchitectureDiff d=diffs.compare(baseline,candidate); int score=0; List<String> reasons=new ArrayList<>();
  if(!d.removedNodes().isEmpty()){score+=Math.min(40,d.removedNodes().size()*10);reasons.add(d.removedNodes().size()+" architecture components removed");}
  if(!d.removedEdges().isEmpty()){score+=Math.min(25,d.removedEdges().size()*5);reasons.add(d.removedEdges().size()+" relationships removed");}
  long critical=d.addedFindings().stream().filter(f->"critical".equalsIgnoreCase(f.severity())||"high".equalsIgnoreCase(f.severity())).count();
  if(critical>0){score+=Math.min(30,(int)critical*15);reasons.add(critical+" new high/critical findings");}
  long api=d.changedNodes().stream().filter(c->"api-endpoint".equals(c.before().type())||"api-endpoint".equals(c.after().type())).count()
      +d.removedNodes().stream().filter(n->"api-endpoint".equals(n.type())).count();
  if(api>0){score+=Math.min(30,(int)api*10);reasons.add(api+" API contracts changed or removed");}
  score=Math.min(score,100); String level=score>=70?"critical":score>=40?"high":score>=15?"medium":"low";
  if(reasons.isEmpty())reasons.add("no structural breaking signals detected");
  return new PrChangeImpact(baseline,candidate,level,score,List.copyOf(reasons),d);
 }
}
