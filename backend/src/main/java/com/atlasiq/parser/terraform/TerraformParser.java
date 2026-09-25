package com.atlasiq.parser.terraform;

import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirNode;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

@ApplicationScoped
public class TerraformParser {
 private static final Pattern BLOCK=Pattern.compile("(?m)^\\s*(resource|data|module|provider|variable|output)\\s+\\\"([^\\\"]+)\\\"(?:\\s+\\\"([^\\\"]+)\\\")?\\s*\\{");
 private static final Pattern REF=Pattern.compile("\\b([A-Za-z0-9_-]+)\\.([A-Za-z0-9_-]+)\\.");
 public TerraformAnalysis parse(Path root){
  var nodes=new ArrayList<QirNode>(); var edges=new ArrayList<QirEdge>(); Map<String,String> addresses=new HashMap<>();
  try(Stream<Path> paths=Files.walk(root)){paths.filter(Files::isRegularFile).filter(this::isTerraform).forEach(p->parseFile(root,p,nodes,addresses));}
  catch(IOException e){throw new IllegalStateException("Unable to scan Terraform files under "+root,e);}
  try(Stream<Path> paths=Files.walk(root)){paths.filter(Files::isRegularFile).filter(this::isTerraform).forEach(p->resolveFile(p,addresses,edges));}
  catch(IOException e){throw new IllegalStateException("Unable to resolve Terraform references under "+root,e);}
  return new TerraformAnalysis(List.copyOf(nodes),List.copyOf(edges),List.of());
 }
 private boolean isTerraform(Path p){return p.getFileName().toString().endsWith(".tf");}
 private void parseFile(Path root,Path path,List<QirNode> nodes,Map<String,String> addresses){
  try{String source=root.relativize(path).toString().replace('\\','/'); Matcher m=BLOCK.matcher(Files.readString(path));
   while(m.find()){String kind=m.group(1),first=m.group(2),second=m.group(3),address=address(kind,first,second),id="terraform:"+address;
    Map<String,Object> meta=new HashMap<>();meta.put("source",source);meta.put("address",address);if(second!=null)meta.put("terraformType",first);
    nodes.add(new QirNode(id,"terraform-"+kind,second==null?first:second,"terraform",Map.copyOf(meta)));addresses.put(address,id);}
  }catch(IOException e){throw new IllegalStateException("Unable to parse Terraform file "+path,e);}
 }
 private void resolveFile(Path path,Map<String,String> addresses,List<QirEdge> edges){
  try{String text=Files.readString(path);Matcher blocks=BLOCK.matcher(text);
   while(blocks.find()){String from=addresses.get(address(blocks.group(1),blocks.group(2),blocks.group(3)));int start=blocks.end(),end=findBlockEnd(text,start-1);if(from==null||end<start)continue;
    Matcher refs=REF.matcher(text.substring(start,end));Set<String> seen=new HashSet<>();
    while(refs.find()){String to=addresses.get(refs.group(1)+"."+refs.group(2));if(to!=null&&!to.equals(from)&&seen.add(to))edges.add(new QirEdge(from,to,"depends_on",Map.of("source","terraform-reference")));}}
  }catch(IOException e){throw new IllegalStateException("Unable to resolve Terraform file "+path,e);}
 }
 private String address(String kind,String first,String second){return switch(kind){case "resource"->first+"."+second;case "data"->"data."+first+"."+second;default->kind+"."+first;};}
 private int findBlockEnd(String text,int open){int depth=0;for(int i=open;i<text.length();i++){char c=text.charAt(i);if(c=='{')depth++;else if(c=='}'&&--depth==0)return i;}return text.length();}
}
