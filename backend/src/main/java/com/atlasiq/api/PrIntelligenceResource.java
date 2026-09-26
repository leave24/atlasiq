package com.atlasiq.api;
import com.atlasiq.pr.*;import jakarta.ws.rs.*;import jakarta.ws.rs.core.MediaType;
@Path("/api/pr-intelligence") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
public class PrIntelligenceResource {private final PrIntelligenceService service;public PrIntelligenceResource(PrIntelligenceService s){service=s;}@POST public Object analyze(Request r){return service.analyze(new PrProvider.PrTarget(r.provider(),r.repository(),r.pullRequestId(),r.token()));}@POST @Path("/publish") public Object publish(Request r){return service.analyzeAndPublish(new PrProvider.PrTarget(r.provider(),r.repository(),r.pullRequestId(),r.token()));}public record Request(String provider,String repository,String pullRequestId,String token){}}
