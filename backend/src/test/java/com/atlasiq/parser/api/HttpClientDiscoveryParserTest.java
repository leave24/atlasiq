package com.atlasiq.parser.api;
import org.junit.jupiter.api.Test; import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*; import static org.junit.jupiter.api.Assertions.*;
class HttpClientDiscoveryParserTest {
 @TempDir Path repo;
 @Test void discoversHttpConsumerWithTraceability() throws Exception {
  Files.writeString(repo.resolve("client.ts"),"const r = fetch('https://orders.internal:8443/api/orders');");
  var n=new HttpClientDiscoveryParser().parse(repo);
  assertEquals(1,n.size()); assertEquals("orders.internal",n.getFirst().metadata().get("targetHost"));
  assertEquals("client.ts",n.getFirst().metadata().get("evidenceFile")); assertEquals(1,n.getFirst().metadata().get("evidenceLine"));
 }
}