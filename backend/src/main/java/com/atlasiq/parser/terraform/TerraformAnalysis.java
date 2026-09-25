package com.atlasiq.parser.terraform;

import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirNode;
import com.atlasiq.qir.Finding;
import java.util.List;

public record TerraformAnalysis(List<QirNode> nodes, List<QirEdge> edges, List<Finding> findings) {}
