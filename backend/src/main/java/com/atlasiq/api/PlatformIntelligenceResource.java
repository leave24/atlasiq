package com.atlasiq.api;
import com.atlasiq.intelligence.*;import com.atlasiq.copilot.*;import com.atlasiq.export.*;import jakarta.ws.rs.*;import jakarta.ws.rs.core.*;import java.io.*;
@Path("/api/platform") @Produces(MediaType.APPLICATION_JSON)
public class PlatformIntelligenceResource {
 private final ServiceCatalogService catalog;private final TimeMachineService time;private final GraphLodService lod;private final AtlasIqCopilotService copilot;private final ArchitectureExportService export;
 public PlatformIntelligenceResource(ServiceCatalogService c,TimeMachineService t,GraphLodService l,AtlasIqCopilotService a,ArchitectureExportService e){catalog=c;time=t;lod=l;copilot=a;export=e;}
 @GET @Path("/catalog/{id}") public Object catalog(@PathParam("id")String id){return catalog.catalog(id);}
 @GET @Path("/search/{id}") public Object search(@PathParam("id")String id,@QueryParam("q")String q){return catalog.search(id,q);}
 @GET @Path("/time-machine") public Object time(@QueryParam("repository")String r,@QueryParam("system")String s,@QueryParam("limit")@DefaultValue("30")int l){return time.timeline(r,s,l);}
 @GET @Path("/graph/{id}") public Object graph(@PathParam("id")String id,@QueryParam("level")String l){return lod.graph(id,l);}
 @POST @Path("/copilot/{id}") @Consumes(MediaType.TEXT_PLAIN) public Object ask(@PathParam("id")String id,String q){return copilot.ask(id,q);}
 @GET @Path("/export/{id}/{format}") public Response export(@PathParam("id")String id,@PathParam("format")String f)throws IOException{return switch(f.toLowerCase()){case"mermaid"->Response.ok(export.mermaid(id),"text/plain").build();case"plantuml"->Response.ok(export.plantUml(id),"text/plain").build();case"svg"->Response.ok(export.svg(id),"image/svg+xml").build();case"png"->Response.ok(export.png(id),"image/png").build();case"pdf"->Response.ok(export.pdf(id),"application/pdf").build();default->throw new BadRequestException("supported formats: mermaid, plantuml, svg, png, pdf");};}
}
