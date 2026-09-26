package com.atlasiq.parser.api;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*; import static org.junit.jupiter.api.Assertions.*;
class ApiDiscoveryParserTest {
 @TempDir Path repo;
 @Test void discoversQuarkusRouteWithLineEvidence() throws Exception {
  Path f=Files.createDirectories(repo.resolve("src")).resolve("Orders.java");
  Files.writeString(f,"@Path(\"/orders\")\nclass Orders {\n @GET\n @Path(\"/{id}\")\n void get(){}\n}");
  var n=new ApiDiscoveryParser().parse(repo);
  assertEquals(1,n.size()); assertEquals("GET /orders/{id}",n.getFirst().name());
  assertEquals("src/Orders.java",n.getFirst().metadata().get("evidenceFile"));
  assertEquals(3,n.getFirst().metadata().get("evidenceLine"));
 }
 @Test void discoversExpressRoute() throws Exception {
  Files.writeString(repo.resolve("app.ts"),"router.post('/payments', handler);");
  var n=new ApiDiscoveryParser().parse(repo);
  assertEquals(1,n.size()); assertEquals("POST /payments",n.getFirst().name());
 }
}