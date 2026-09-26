package com.atlasiq.api;
import com.atlasiq.enterprise.*;import jakarta.ws.rs.*;import jakarta.ws.rs.core.MediaType;import java.util.*;
@Path("/api/v1/enterprise") @Produces(MediaType.APPLICATION_JSON) @Consumes(MediaType.APPLICATION_JSON)
public class EnterpriseResource {
 private final TenantContext tenant;private final OrganizationService orgs;private final AuditLogService audit;private final WebhookService webhooks;private final BillingService billing;
 public EnterpriseResource(TenantContext t,OrganizationService o,AuditLogService a,WebhookService w,BillingService b){tenant=t;orgs=o;audit=a;webhooks=w;billing=b;}
 @POST @Path("/organizations") public Object org(Name r){var x=orgs.createOrg(r.name());audit.record(tenant.organizationId(),tenant.workspaceId(),tenant.subject(),"organization.create",x.id());return x;}
 @POST @Path("/organizations/{org}/workspaces") public Object workspace(@PathParam("org")String org,Name r){var x=orgs.createWorkspace(org,r.name());audit.record(org,x.id(),tenant.subject(),"workspace.create",x.id());return x;}
 @GET @Path("/audit") public Object audit(@QueryParam("limit")@DefaultValue("100")int l){return audit.list(tenant.organizationId(),tenant.workspaceId(),l);}
 @GET @Path("/webhooks") public Object hooks(){return webhooks.list(tenant.organizationId(),tenant.workspaceId());}
 @POST @Path("/webhooks") public Object hook(WebhookRequest r){var x=webhooks.register(tenant.organizationId(),tenant.workspaceId(),r.url(),r.events());audit.record(tenant.organizationId(),tenant.workspaceId(),tenant.subject(),"webhook.create",x.id());return x;}
 @GET @Path("/billing") public Object billing(){return billing.get(tenant.organizationId());}
 @PUT @Path("/billing") public Object billing(BillingRequest r){var x=billing.assign(tenant.organizationId(),r.plan(),r.seats());audit.record(tenant.organizationId(),tenant.workspaceId(),tenant.subject(),"billing.update",r.plan());return x;}
 public record Name(String name){}public record WebhookRequest(String url,Set<String> events){}public record BillingRequest(String plan,int seats){}
}
