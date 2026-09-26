package com.atlasiq.enterprise;
import jakarta.annotation.Priority;import jakarta.inject.Inject;import jakarta.ws.rs.*;import jakarta.ws.rs.container.*;import jakarta.ws.rs.core.*;import jakarta.ws.rs.ext.Provider;
@Provider @Priority(Priorities.AUTHORIZATION) public class TenantFilter implements ContainerRequestFilter {
 @Inject TenantContext context;
 public void filter(ContainerRequestContext r){if(r.getUriInfo().getPath().startsWith("q/"))return;String org=r.getHeaderString("X-AtlasIQ-Organization");String ws=r.getHeaderString("X-AtlasIQ-Workspace");SecurityContext sc=r.getSecurityContext();String sub=sc!=null&&sc.getUserPrincipal()!=null?sc.getUserPrincipal().getName():"anonymous";if(org==null||org.isBlank()||ws==null||ws.isBlank())throw new BadRequestException("organization and workspace headers are required");context.set(org.trim(),ws.trim(),sub);}
}
