package com.atlasiq.qir;
import java.util.*;
public final class CanonicalMetadata {private CanonicalMetadata(){}
 public static Map<String,Object> of(Map<String,Object> metadata){if(metadata==null||metadata.isEmpty())return Map.of();TreeMap<String,Object> sorted=new TreeMap<>();metadata.forEach((k,v)->{if(!Set.of("workspace","scannedAt","timestamp").contains(k))sorted.put(k,canonical(v));});return Collections.unmodifiableMap(sorted);}
 private static Object canonical(Object v){if(v instanceof Map<?,?>m){TreeMap<String,Object> out=new TreeMap<>();m.forEach((k,x)->out.put(String.valueOf(k),canonical(x)));return out;}if(v instanceof Collection<?>c)return c.stream().map(CanonicalMetadata::canonical).toList();return v;}
}
