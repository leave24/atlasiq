package com.atlasiq.api;
import com.atlasiq.persistence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.List;

@Path("/api/analyses")
@Produces(MediaType.APPLICATION_JSON)
public class AnalysisResource {
 private final AnalysisStore store;
 public AnalysisResource(AnalysisStore store){this.store=store;}
 @GET public List<StoredAnalysis> recent(@QueryParam("limit") @DefaultValue("20") int limit){return store.recent(limit);}
 @GET @Path("/{id}") public StoredAnalysis get(@PathParam("id") String id){
  return store.find(id).orElseThrow(()->new NotFoundException("analysis not found"));
 }
}
