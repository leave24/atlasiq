package com.atlasiq.api;
import com.atlasiq.persistence.*;import com.atlasiq.enterprise.*;import jakarta.ws.rs.*;import jakarta.ws.rs.core.MediaType;
@Path("/api/v1") @Produces(MediaType.APPLICATION_JSON)
public class PublicApiResource {
 private final AnalysisStore store;private final TenantContext tenant;private final AuditLogService audit;
 public PublicApiResource(AnalysisStore s,TenantContext t,AuditLogService a){store=s;tenant=t;audit=a;}
 @GET @Path("/analyses/{id}") public Object analysis(@PathParam("id")String id){var x=store.find(id).orElseThrow(()->new NotFoundException("analysis not found"));audit.record(tenant.organizationId(),tenant.workspaceId(),tenant.subject(),"analysis.read",id);return x;}
 @GET @Path("/analyses") public Object analyses(@QueryParam("repository")String r,@QueryParam("system")String s,@QueryParam("limit")@DefaultValue("50")int l){audit.record(tenant.organizationId(),tenant.workspaceId(),tenant.subject(),"analysis.list",r==null?"*":r);return store.history(r,s,l);}
}
