package com.atlasiq.parser.api;

import com.atlasiq.qir.QirNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

@ApplicationScoped
public class HttpClientDiscoveryParser {
    private static final Set<String> EXCLUDED=Set.of(".git","target","build","dist","node_modules","vendor",".next");
    private static final Pattern CALL=Pattern.compile("(?:fetch|axios\\.(?:get|post|put|delete|patch)|\\.(?:get|post|put|delete|patch)\\()\\s*\\(?\\s*[\"'](https?://[A-Za-z0-9._-]+(?::\\d+)?[^\\s\"']*)[\"']",Pattern.CASE_INSENSITIVE);

    public List<QirNode> parse(Path repository){
        List<QirNode> out=new ArrayList<>(); Path root=repository.toAbsolutePath().normalize();
        try(Stream<Path> files=Files.walk(root,10)){
            files.filter(p->safe(root,p)).forEach(p->parse(root,p,out));
        }catch(IOException e){throw new IllegalStateException("failed to inspect HTTP clients",e);}
        return List.copyOf(out);
    }
    private void parse(Path root,Path file,List<QirNode> out){
        String n=file.getFileName().toString();
        if(!(n.endsWith(".java")||n.endsWith(".js")||n.endsWith(".ts")||n.endsWith(".tsx")))return;
        try{
            List<String> lines=Files.readAllLines(file); String rel=root.relativize(file).toString().replace('\\','/');
            for(int i=0;i<lines.size();i++){
                Matcher m=CALL.matcher(lines.get(i)); int j=0;
                while(m.find()){
                    String raw=m.group(1), sanitized=sanitize(raw), host=host(sanitized);
                    out.add(new QirNode("http-client:"+rel+":"+(i+1)+":"+j++,"http-client",host,"source",
                            Map.of("targetUrl",sanitized,"targetHost",host,"evidenceFile",rel,"evidenceLine",i+1,"evidenceKind","source-http-call")));
                }
            }
        }catch(IOException ignored){}
    }
    private String sanitize(String v){int s=v.indexOf("://")+3;int e=v.length();for(int i=s;i<v.length();i++){char c=v.charAt(i);if(c=='/'||c=='?'||c=='#'){e=i;break;}}return v.substring(0,s)+v.substring(s,e);}
    private String host(String v){String a=v.substring(v.indexOf("://")+3);int c=a.lastIndexOf(':');return c>0&&a.substring(c+1).chars().allMatch(Character::isDigit)?a.substring(0,c):a;}
    private boolean safe(Path root,Path p){Path rel=root.relativize(p.toAbsolutePath().normalize());for(Path x:rel)if(EXCLUDED.contains(x.toString()))return false;return Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&!Files.isSymbolicLink(p);}
}