package com.atlasiq.api;
import com.atlasiq.persistence.*;import com.atlasiq.twin.*;import jakarta.ws.rs.*;import jakarta.ws.rs.core.MediaType;import java.time.*;import java.util.*;
@Path("/api/twin") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class ArchitectureTwinResource {private final AnalysisStore store;private final ArchitectureSimulationService simulation;private final EvidenceConfidenceService truth;private final ArchitectureHotspotService hotspots;private final ArchitectureBudgetService budgets;private final ArchitectureMemoryService memory;private final ArchitectureBlackBoxService blackbox;private final AdrSuggestionService adrs;private final AgentGuardService guard;
 public ArchitectureTwinResource(AnalysisStore s,ArchitectureSimulationService si,EvidenceConfidenceService t,ArchitectureHotspotService h,ArchitectureBudgetService b,ArchitectureMemoryService m,ArchitectureBlackBoxService bb,AdrSuggestionService a,AgentGuardService g){store=s;simulation=si;truth=t;hotspots=h;budgets=b;memory=m;blackbox=bb;adrs=a;guard=g;}
 @POST @Path("/{id}/simulate") public Object simulate(@PathParam("id")String id,ArchitectureSimulationService.Scenario r){return simulation.simulate(id,r);}
 @GET @Path("/{id}/truth") public Object truth(@PathParam("id")String id){return truth.assess(store.find(id).orElseThrow(()->new NotFoundException("analysis not found")).model());}
 @GET @Path("/{id}/hotspots") public Object hotspots(@PathParam("id")String id){return hotspots.analyze(id);}
 @POST @Path("/budget") public Object budget(ChangeBudget r){return budgets.evaluate(r.baseAnalysisId(),r.headAnalysisId(),r.budget());}
 @GET @Path("/memory") public Object memory(@QueryParam("repository")String repo,@QueryParam("system")String system,@QueryParam("node")String node,@QueryParam("limit")@DefaultValue("100")int limit){return memory.remember(repo,system,node,limit);}
 @GET @Path("/black-box") public Object blackbox(@QueryParam("repository")String repo,@QueryParam("system")String system,@QueryParam("before")String before,@QueryParam("after")String after){return blackbox.timeline(repo,system,Instant.parse(before),Instant.parse(after));}
 @GET @Path("/adr-suggestions") public Object adrs(@QueryParam("from")String from,@QueryParam("to")String to){return adrs.suggest(from,to);}
 @POST @Path("/{id}/agent-guard") public Object guard(@PathParam("id")String id,Guard r){return guard.preflight(id,r.targetNodeIds());}
 public record ChangeBudget(String baseAnalysisId,String headAnalysisId,ArchitectureBudgetService.Budget budget){}public record Guard(List<String>targetNodeIds){}
}
