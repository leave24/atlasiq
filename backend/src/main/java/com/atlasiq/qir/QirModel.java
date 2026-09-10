package com.atlasiq.qir;

import java.util.List;
import java.util.Map;

public record QirModel(
        String repository,
        String ref,
        List<QirNode> nodes,
        List<QirEdge> edges,
        List<Finding> findings) {

    public record QirNode(String id, String type, String name, String technology, Map<String, Object> metadata) {}
    public record QirEdge(String from, String to, String relationship, Map<String, Object> metadata) {}
    public record Finding(String id, String severity, String category, String resourceId, String title, String recommendation) {}
}
