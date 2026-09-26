package com.atlasiq.api;
import com.atlasiq.intelligence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/intelligence")
@Produces(MediaType.APPLICATION_JSON)
public class ArchitectureIntelligenceResource {
 private final ArchitectureDiffService diffs; private final PrChangeImpactService impact; private final BlastRadiusService blast; private final ArchitectureDriftService drift; private final SecurityGraphService security;
 public ArchitectureIntelligenceResource(ArchitectureDiffService diffs,PrChangeImpactService impact,BlastRadiusService blast,ArchitectureDriftService drift,SecurityGraphService security){this.diffs=diffs;this.impact=impact;this.blast=blast;this.drift=drift;this.security=security;}
 @GET @Path("/diff") public ArchitectureDiff diff(@QueryParam("from") String from,@QueryParam("to") String to){required(from,to);return diffs.compare(from,to);}
 @GET @Path("/pr-impact") public PrChangeImpact impact(@QueryParam("baseline") String baseline,@QueryParam("candidate") String candidate){required(baseline,candidate);return impact.assess(baseline,candidate);}
 @GET @Path("/blast-radius") public BlastRadiusService.BlastRadius blast(@QueryParam("analysis") String analysis,@QueryParam("node") String node,@QueryParam("depth") @DefaultValue("3") int depth){required(analysis,node);return blast.analyze(analysis,node,depth);}
 @GET @Path("/drift") public ArchitectureDriftService.Drift drift(@QueryParam("desired") String desired,@QueryParam("observed") String observed){required(desired,observed);return drift.detect(desired,observed);}
 @GET @Path("/security-graph") public SecurityGraphService.SecurityGraph security(@QueryParam("analysis") String analysis){if(analysis==null||analysis.isBlank())throw new BadRequestException("analysis id is required");return security.graph(analysis);}
 private void required(String a,String b){if(a==null||a.isBlank()||b==null||b.isBlank())throw new BadRequestException("both analysis ids are required");}
}
