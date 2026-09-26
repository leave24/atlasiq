package com.atlasiq.intelligence;
import com.atlasiq.persistence.*; import com.atlasiq.qir.*; import org.junit.jupiter.api.Test;
import java.time.Instant; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
class PrChangeImpactServiceTest {
 @Test void raisesImpactForRemovedApiAndNewCriticalFinding(){
  var api=new QirNode("api","api-endpoint","GET /orders","java",Map.of());
  var before=model(List.of(api),List.of()); var critical=new Finding("f","critical","security","repo","exposure","fix");
  var after=model(List.of(),List.of(critical)); var store=new Store(Map.of("base",before,"candidate",after));
  var result=new PrChangeImpactService(new ArchitectureDiffService(store)).assess("base","candidate");
  assertTrue(result.score()>=35); assertTrue(result.reasons().stream().anyMatch(x->x.contains("API")));
 }
 static QirModel model(List<QirNode> n,List<Finding> f){return new QirModel("repo","main",n,List.of(),f,new QirScope("sys","repo:id"));}
 static class Store implements AnalysisStore {
  final Map<String,QirModel> m;Store(Map<String,QirModel> m){this.m=m;} public StoredAnalysis save(QirModel x){throw new UnsupportedOperationException();}
  public Optional<StoredAnalysis> find(String id){return Optional.ofNullable(m.get(id)).map(x->new StoredAnalysis(id,Instant.EPOCH,x));}
  public List<StoredAnalysis> recent(int l){return List.of();}public List<StoredAnalysis> history(String r,String s,int l){return List.of();}
 }
}