package com.atlasiq.scanner;
import java.time.Instant;
public record AnalysisJob(String id,String repository,String ref,String system,Status status,Instant createdAt,String analysisId,String error){public enum Status{QUEUED,RUNNING,SUCCEEDED,FAILED,TIMED_OUT}}
