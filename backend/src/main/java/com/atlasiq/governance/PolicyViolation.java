package com.atlasiq.governance;
import java.util.*;
public record PolicyViolation(String policyId,String severity,String resourceId,String message,List<String> evidence){}