package com.atlasiq.scanner;
import jakarta.enterprise.context.ApplicationScoped;import java.time.Instant;import java.util.*;import java.util.concurrent.*;
@ApplicationScoped public class AnalysisJobQueue {private final BlockingQueue<AnalysisJob> queue=new LinkedBlockingQueue<>(1000);private final ConcurrentMap<String,AnalysisJob> jobs=new ConcurrentHashMap<>();
 public AnalysisJob enqueue(ScanRequest r){String id=UUID.randomUUID().toString();var j=new AnalysisJob(id,r.repository(),r.ref(),r.system(),AnalysisJob.Status.QUEUED,Instant.now(),null,null);if(!queue.offer(j))throw new IllegalStateException("analysis queue is full");jobs.put(id,j);return j;}
 public Optional<AnalysisJob> find(String id){return Optional.ofNullable(jobs.get(id));}public AnalysisJob take()throws InterruptedException{return queue.take();}public void update(AnalysisJob j){jobs.put(j.id(),j);}
}
