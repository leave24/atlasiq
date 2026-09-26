package com.atlasiq.parser.database;
import com.atlasiq.qir.*; import jakarta.enterprise.context.ApplicationScoped;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.regex.*;import java.util.stream.Stream;
@ApplicationScoped public class DatabaseIntelligenceParser {
 private static final Set<String> X=Set.of(".git","target","build","dist","node_modules","vendor",".next");
 private static final Pattern JDBC=Pattern.compile("jdbc:(postgresql|mysql|sqlserver):[^\\s\"']+",Pattern.CASE_INSENSITIVE);
 private static final Pattern SQL=Pattern.compile("(?i)\\b(?:from|join|update|into)\\s+([A-Za-z_][A-Za-z0-9_.]*)");
 public Result parse(Path root){List<QirNode> n=new ArrayList<>();List<QirEdge> e=new ArrayList<>();
  try(Stream<Path>s=Files.walk(root,10)){s.filter(p->safe(root,p)).forEach(p->{try{String rel=root.relativize(p).toString().replace('\\','/');List<String>ls=Files.readAllLines(p);for(int i=0;i<ls.size();i++){Matcher j=JDBC.matcher(ls.get(i));while(j.find()){String id="database:"+j.group(1).toLowerCase();n.add(new QirNode(id,"database",j.group(1),j.group(1),Map.of("evidenceFile",rel,"evidenceLine",i+1)));}Matcher q=SQL.matcher(ls.get(i));while(q.find()){String table=q.group(1);String id="db-object:"+table.toLowerCase();n.add(new QirNode(id,"database-object",table,"sql",Map.of("evidenceFile",rel,"evidenceLine",i+1)));}}}catch(Exception ignored){}});}catch(IOException x){throw new IllegalStateException(x);}
  return new Result(n.stream().distinct().toList(),List.copyOf(e));}
 private boolean safe(Path r,Path p){Path rel=r.relativize(p);for(Path x:rel)if(X.contains(x.toString()))return false;return Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&!Files.isSymbolicLink(p)&&Files.size(p)<2_000_000;}
 public record Result(List<QirNode> nodes,List<QirEdge> edges){}
}