package com.atlasiq.api;
import com.atlasiq.intelligence.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/intelligence")
@Produces(MediaType.APPLICATION_JSON)
public class ArchitectureIntelligenceResource {
 private final ArchitectureDiffService diffs; private final PrChangeImpactService impact;
 public ArchitectureIntelligenceResource(ArchitectureDiffService diffs,PrChangeImpactService impact){this.diffs=diffs;this.impact=impact;}
 @GET @Path("/diff") public ArchitectureDiff diff(@QueryParam("from") String from,@QueryParam("to") String to){required(from,to);return diffs.compare(from,to);}
 @GET @Path("/pr-impact") public PrChangeImpact impact(@QueryParam("baseline") String baseline,@QueryParam("candidate") String candidate){required(baseline,candidate);return impact.assess(baseline,candidate);}
 private void required(String a,String b){if(a==null||a.isBlank()||b==null||b.isBlank())throw new BadRequestException("both analysis ids are required");}
}
