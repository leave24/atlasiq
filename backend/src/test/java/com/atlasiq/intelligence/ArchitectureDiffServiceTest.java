package com.atlasiq.intelligence;
import com.atlasiq.persistence.*; import com.atlasiq.qir.*; import org.junit.jupiter.api.Test;
import java.time.Instant; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
class ArchitectureDiffServiceTest {
 @Test void detectsStructuralChanges(){
  QirNode a=new QirNode("api","api-endpoint","GET /orders","java",Map.of("evidenceLine",1));
  QirNode b=new QirNode("api","api-endpoint","GET /orders/{id}","java",Map.of("evidenceLine",2));
  MemoryStore s=new MemoryStore(Map.of("a",model(List.of(a)),"b",model(List.of(b))));
  var d=new ArchitectureDiffService(s).compare("a","b");
  assertEquals(1,d.changedNodes().size()); assertEquals(1,d.changeCount());
 }
 static QirModel model(List<QirNode> n){return new QirModel("repo","main",n,List.of(),List.of(),new QirScope("sys","repo:id"));}
 static class MemoryStore implements AnalysisStore {
  final Map<String,QirModel> m; MemoryStore(Map<String,QirModel> m){this.m=m;}
  public StoredAnalysis save(QirModel x){throw new UnsupportedOperationException();}
  public Optional<StoredAnalysis> find(String id){return Optional.ofNullable(m.get(id)).map(x->new StoredAnalysis(id,Instant.EPOCH,x));}
  public List<StoredAnalysis> recent(int l){return List.of();} public List<StoredAnalysis> history(String r,String s,int l){return List.of();}
 }
}