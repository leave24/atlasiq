package com.atlasiq.parser.supplychain;
import com.atlasiq.qir.*;import com.fasterxml.jackson.databind.*;import jakarta.enterprise.context.ApplicationScoped;import java.io.*;import java.nio.file.*;import java.util.*;
@ApplicationScoped public class SupplyChainParser {
 private final ObjectMapper json=new ObjectMapper();
 public List<QirNode> parse(Path root){List<QirNode> out=new ArrayList<>();Path p=root.resolve("package.json");if(Files.isRegularFile(p))try{JsonNode d=json.readTree(p.toFile());for(String section:List.of("dependencies","devDependencies"))d.path(section).fields().forEachRemaining(x->out.add(node("npm",x.getKey(),x.getValue().asText(),"package.json")));}catch(IOException ignored){}
  Path pom=root.resolve("pom.xml");if(Files.isRegularFile(pom))try{String x=Files.readString(pom);java.util.regex.Matcher m=java.util.regex.Pattern.compile("<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*(?:<version>([^<]+)</version>)?").matcher(x);while(m.find())out.add(node("maven",m.group(1)+":"+m.group(2),m.group(3)==null?"managed":m.group(3),"pom.xml"));}catch(IOException ignored){}
  return List.copyOf(out);}
 private QirNode node(String ecosystem,String name,String version,String file){return new QirNode("package:"+ecosystem+":"+name,"software-package",name,ecosystem,Map.of("version",version,"ecosystem",ecosystem,"evidenceFile",file,"sbom","cyclonedx-compatible"));}
}