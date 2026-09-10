package com.atlasiq.parser.kubernetes;

import com.atlasiq.qir.Finding;
import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirNode;

import java.util.List;

public record KubernetesAnalysis(
        List<QirNode> nodes,
        List<QirEdge> edges,
        List<Finding> findings) {}
