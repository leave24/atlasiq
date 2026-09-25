package com.atlasiq.parser.githubactions;

import com.atlasiq.qir.Finding;
import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirNode;

import java.util.List;

public record GitHubActionsAnalysis(List<QirNode> nodes, List<QirEdge> edges, List<Finding> findings) {
}
