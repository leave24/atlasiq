package com.atlasiq.qir;
import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;
public final class QirIdentity {private QirIdentity(){}
 public static String node(String scope,String type,String naturalKey){return id("node",scope,type,naturalKey);}
 public static String edge(String scope,String from,String relationship,String to){return id("edge",scope,from,relationship,to);}
 private static String id(String prefix,String...parts){try{var md=MessageDigest.getInstance("SHA-256");for(String p:parts){md.update(String.valueOf(p).trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));md.update((byte)0);}return prefix+":"+HexFormat.of().formatHex(md.digest()).substring(0,24);}catch(Exception e){throw new IllegalStateException(e);}}
}
