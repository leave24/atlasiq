package com.atlasiq.scanner;
import com.atlasiq.qir.*;import java.nio.file.Path;import java.util.*;
public interface RepositoryAnalysisContributor {String id();Contribution analyze(Path repositoryPath);record Contribution(List<QirNode> nodes,List<QirEdge> edges,List<Finding> findings){public static Contribution nodes(List<QirNode> nodes){return new Contribution(nodes,List.of(),List.of());}}}
