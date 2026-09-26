package com.atlasiq.api;
import com.atlasiq.intelligence.RuntimeKubernetesCorrelator;import com.atlasiq.qir.*;import jakarta.ws.rs.*;import jakarta.ws.rs.core.MediaType;import java.util.*;
@Path("/api/runtime/kubernetes") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
public class RuntimeKubernetesResource {
 private final RuntimeKubernetesCorrelator c;public RuntimeKubernetesResource(RuntimeKubernetesCorrelator c){this.c=c;}
 @POST @Path("/correlate") public List<QirEdge> correlate(List<QirNode> runtimeAndDeclared){return c.correlate(runtimeAndDeclared);}
}