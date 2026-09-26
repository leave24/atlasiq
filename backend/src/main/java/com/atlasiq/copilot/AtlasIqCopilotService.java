package com.atlasiq.copilot;
import com.atlasiq.persistence.*;import com.atlasiq.qir.*;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class AtlasIqCopilotService {
 private final AnalysisStore store;public AtlasIqCopilotService(AnalysisStore store){this.store=store;}
 public Answer ask(String analysisId,String question){QirModel m=store.find(analysisId).orElseThrow(()->new NoSuchElementException("analysis not found")).model();Set<String> terms=new LinkedHashSet<>(Arrays.asList(question.toLowerCase().split("[^a-z0-9_-]+")));terms.removeIf(x->x.length()<3);List<Evidence> evidence=m.nodes().stream().map(n->new Evidence(n,score(n,terms))).filter(e->e.score>0).sorted(Comparator.comparingInt(Evidence::score).reversed()).limit(12).toList();String summary=evidence.isEmpty()?"No matching architecture evidence was found in this snapshot.":"Found "+evidence.size()+" grounded architecture facts relevant to the question.";return new Answer(summary,evidence.stream().map(Evidence::node).toList(),false);}
 private int score(QirNode n,Set<String> t){String h=(n.id()+" "+n.name()+" "+n.type()+" "+n.technology()+" "+n.metadata()).toLowerCase();return(int)t.stream().filter(h::contains).count();}
 private record Evidence(QirNode node,int score){}public record Answer(String answer,List<QirNode> evidence,boolean generative){}
}
