package com.atlasiq.parser.api;

import com.atlasiq.qir.QirNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

@ApplicationScoped
public class ApiDiscoveryParser {
    private static final Set<String> EXCLUDED = Set.of(".git","target","build","dist","node_modules","vendor",".next");
    private static final Pattern JAVA_CLASS_PATH = Pattern.compile("@(?:Path|RequestMapping)\\(\\s*[\"']([^\"']+)[\"']");
    private static final Pattern JAVA_METHOD = Pattern.compile("@(GET|POST|PUT|DELETE|PATCH)|@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping|RequestMapping)(?:\\(\\s*[\"']([^\"']*)[\"'])?");
    private static final Pattern EXPRESS = Pattern.compile("(?:app|router)\\.(get|post|put|delete|patch)\\(\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public List<QirNode> parse(Path repository) {
        List<QirNode> result = new ArrayList<>();
        Path root = repository.toAbsolutePath().normalize();
        try (Stream<Path> files = Files.walk(root, 10)) {
            files.filter(p -> safe(root,p)).forEach(p -> parseFile(root,p,result));
        } catch (IOException e) { throw new IllegalStateException("failed to inspect API source", e); }
        return List.copyOf(result);
    }

    private void parseFile(Path root, Path file, List<QirNode> out) {
        String name=file.getFileName().toString();
        if (!(name.endsWith(".java")||name.endsWith(".js")||name.endsWith(".ts")||name.endsWith(".tsx"))) return;
        try {
            List<String> lines=Files.readAllLines(file);
            String relative=root.relativize(file).toString().replace('\\','/');
            String base="";
            String pendingMethod=null;
            int pendingLine=-1;
            String pendingPath="";
            for(int i=0;i<lines.size();i++){
                String line=lines.get(i);
                Matcher cp=JAVA_CLASS_PATH.matcher(line);
                if(cp.find() && pendingMethod == null) base=cp.group(1);

                Matcher jm=JAVA_METHOD.matcher(line);
                if(jm.find()){
                    String method=jm.group(1)!=null?jm.group(1):mappingMethod(jm.group(2));
                    String sub=jm.group(3)==null?"":jm.group(3);
                    if(method!=null){
                        if(pendingMethod!=null) add(out,relative,pendingLine,pendingMethod,join(base,pendingPath),"java");
                        pendingMethod=method;
                        pendingLine=i+1;
                        pendingPath=sub;
                    }
                } else if(pendingMethod!=null) {
                    Matcher methodPath=JAVA_CLASS_PATH.matcher(line);
                    if(methodPath.find()) pendingPath=methodPath.group(1);
                    if(!line.trim().startsWith("@") && !line.isBlank()){
                        add(out,relative,pendingLine,pendingMethod,join(base,pendingPath),"java");
                        pendingMethod=null; pendingLine=-1; pendingPath="";
                    }
                }

                Matcher ex=EXPRESS.matcher(line);
                while(ex.find()) add(out,relative,i+1,ex.group(1).toUpperCase(Locale.ROOT),ex.group(2),"express");
            }
            if(pendingMethod!=null) add(out,relative,pendingLine,pendingMethod,join(base,pendingPath),"java");
        } catch(IOException ignored){}
    }

    private void add(List<QirNode> out,String file,int line,String method,String path,String framework){
        if(method==null||path==null||path.isBlank()) return;
        String id="api:"+file+":"+line+":"+method;
        out.add(new QirNode(id,"api-endpoint",method+" "+path,framework,Map.of(
                "httpMethod",method,"path",path,"framework",framework,
                "evidenceFile",file,"evidenceLine",line,"evidenceKind","source-route")));
    }
    private String mappingMethod(String value){
        if(value==null||value.equals("RequestMapping")) return null;
        return value.replace("Mapping","").toUpperCase(Locale.ROOT);
    }
    private String join(String a,String b){
        String v=(a==null?"":a)+(b==null?"":b);
        if(v.isBlank()) return "/";
        return ("/"+v).replaceAll("/+","/");
    }
    private boolean safe(Path root,Path p){
        Path rel=root.relativize(p.toAbsolutePath().normalize());
        for(Path part:rel) if(EXCLUDED.contains(part.toString())) return false;
        return Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&!Files.isSymbolicLink(p);
    }
}