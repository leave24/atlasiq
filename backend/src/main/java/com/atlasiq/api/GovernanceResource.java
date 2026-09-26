package com.atlasiq.api;
import com.atlasiq.governance.*;import jakarta.ws.rs.*;import jakarta.ws.rs.core.MediaType;
@Path("/api/governance") @Produces(MediaType.APPLICATION_JSON)
public class GovernanceResource {private final GovernanceService service;public GovernanceResource(GovernanceService s){service=s;}@POST @Path("/evaluate/{analysis}") @Consumes("application/yaml") public Object evaluate(@PathParam("analysis")String id,String yaml){return service.evaluate(id,yaml);}}