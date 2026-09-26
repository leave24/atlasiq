package com.atlasiq.qir;
import org.junit.jupiter.api.Test; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
class ApiConsumerCorrelatorTest {
 @Test void correlatesOnlyUniqueExplicitHost() {
  var c=new QirNode("c","http-client","orders.internal","source",Map.of("targetHost","orders.internal","evidenceFile","client.ts","evidenceLine",4));
  var a=new QirNode("a","api-endpoint","GET /orders","java",Map.of("hostname","orders.internal"));
  var e=new ApiConsumerCorrelator().correlate(List.of(c,a));
  assertEquals(1,e.size()); assertEquals("calls-api",e.getFirst().relationship()); assertEquals(4,e.getFirst().metadata().get("evidenceLine"));
 }
 @Test void refusesAmbiguousApiHost() {
  var c=new QirNode("c","http-client","api","source",Map.of("targetHost","api"));
  var a=new QirNode("a","api-endpoint","api","java",Map.of());
  var b=new QirNode("b","api-endpoint","api","java",Map.of());
  assertTrue(new ApiConsumerCorrelator().correlate(List.of(c,a,b)).isEmpty());
 }
}