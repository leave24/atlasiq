package com.atlasiq.governance;
import java.util.*;
public record ArchitecturePolicy(String id,String description,String severity,Rule rule){
 public record Rule(String kind,String fromType,String toType,String relationship,List<String> values,String metadataKey,String metadataValue){}
}