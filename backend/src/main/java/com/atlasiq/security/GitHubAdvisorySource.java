package com.atlasiq.security;
import com.atlasiq.qir.QirNode;import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class GitHubAdvisorySource implements VulnerabilitySource {public String id(){return "github-advisory";}public List<Vulnerability> query(QirNode p){return List.of();}}
