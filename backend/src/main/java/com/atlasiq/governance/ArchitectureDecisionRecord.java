package com.atlasiq.governance;
import java.util.*;
public record ArchitectureDecisionRecord(String id,String title,String status,String decision,List<String> qirSelectors,List<String> policyIds,String source){}