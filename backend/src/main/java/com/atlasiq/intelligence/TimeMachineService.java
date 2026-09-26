package com.atlasiq.intelligence;
import com.atlasiq.persistence.*;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class TimeMachineService {
 private final AnalysisStore store;private final ArchitectureDiffService diffs;public TimeMachineService(AnalysisStore store,ArchitectureDiffService diffs){this.store=store;this.diffs=diffs;}
 public Timeline timeline(String repository,String system,int limit){List<StoredAnalysis> h=store.history(repository,system,limit);List<Transition> t=new ArrayList<>();for(int i=h.size()-1;i>0;i--){StoredAnalysis from=h.get(i),to=h.get(i-1);var d=diffs.compare(from.id(),to.id());t.add(new Transition(from.id(),to.id(),from.createdAt(),to.createdAt(),d.changeCount()));}return new Timeline(h,t);}
 public record Transition(String from,String to,java.time.Instant fromTime,java.time.Instant toTime,int changes){} public record Timeline(List<StoredAnalysis> snapshots,List<Transition> transitions){}
}
