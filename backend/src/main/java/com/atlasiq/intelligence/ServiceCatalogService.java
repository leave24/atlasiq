package com.atlasiq.intelligence;
import com.atlasiq.persistence.*;import com.atlasiq.qir.*;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class ServiceCatalogService {
 private final AnalysisStore store;public ServiceCatalogService(AnalysisStore store){this.store=store;}
 public List<QirNode> catalog(String id){QirModel m=model(id);return m.nodes().stream().filter(n->Set.of("repository","service","api-contract","asyncapi-contract","cloud-resource","team").contains(n.type())).toList();}
 public List<QirNode> search(String id,String query){String q=query==null?"":query.toLowerCase(Locale.ROOT);return model(id).nodes().stream().filter(n->n.id().toLowerCase().contains(q)||n.name().toLowerCase().contains(q)||n.type().toLowerCase().contains(q)||n.technology().toLowerCase().contains(q)).limit(200).toList();}
 private QirModel model(String id){return store.find(id).orElseThrow(()->new NoSuchElementException("analysis not found")).model();}
}
