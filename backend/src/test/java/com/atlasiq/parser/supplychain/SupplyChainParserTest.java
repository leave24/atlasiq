package com.atlasiq.parser.supplychain;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.io.TempDir;import java.nio.file.*;import static org.junit.jupiter.api.Assertions.*;
class SupplyChainParserTest{@TempDir Path root;@Test void buildsPackageInventory()throws Exception{Files.writeString(root.resolve("package.json"),"{\"dependencies\":{\"react\":\"19.0.0\"}}");var n=new SupplyChainParser().parse(root);assertEquals(1,n.size());assertEquals("software-package",n.getFirst().type());assertEquals("19.0.0",n.getFirst().metadata().get("version"));}}
