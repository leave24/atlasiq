package com.atlasiq.intelligence;
import com.atlasiq.qir.*;import org.junit.jupiter.api.Test;import java.util.*;import static org.junit.jupiter.api.Assertions.*;
class RuntimeKubernetesCorrelatorTest{@Test void correlatesDeclaredAndObservedWorkload(){var d=new QirNode("d","kubernetes-deployment","api","kubernetes",Map.of("namespace","prod"));var r=new QirNode("r","kubernetes-runtime-workload","api","kubernetes-runtime",Map.of("namespace","prod"));var e=new RuntimeKubernetesCorrelator().correlate(List.of(d,r));assertEquals(1,e.size());assertEquals("observed_as",e.getFirst().relationship());}}
