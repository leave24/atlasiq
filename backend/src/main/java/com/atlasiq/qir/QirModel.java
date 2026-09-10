package com.atlasiq.qir;

import java.util.List;

public record QirModel(
        String repository,
        String ref,
        List<QirNode> nodes,
        List<QirEdge> edges,
        List<Finding> findings) {}

