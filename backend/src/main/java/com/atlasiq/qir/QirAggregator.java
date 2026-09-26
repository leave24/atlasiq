package com.atlasiq.qir;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

@ApplicationScoped
public class QirAggregator {

    private final CrossRepositoryDependencyResolver dependencyResolver;

    public QirAggregator(CrossRepositoryDependencyResolver dependencyResolver) {
        this.dependencyResolver = dependencyResolver;
    }

    public QirModel aggregate(String system, List<QirModel> repositories) {
        if (system == null || system.isBlank()) throw new IllegalArgumentException("system is required");
        if (repositories == null || repositories.isEmpty()) throw new IllegalArgumentException("at least one repository QIR is required");

        String systemName = system.trim();
        String systemId = "system:" + encode(systemName);
        var nodes = new ArrayList<QirNode>();
        var edges = new ArrayList<QirEdge>();
        var findings = new ArrayList<Finding>();
        Set<String> repositoryIds = new HashSet<>();

        nodes.add(new QirNode(systemId, "system", systemName, "atlasiq",
                Map.of("repositoryCount", repositories.size())));

        for (QirModel model : repositories) {
            if (model == null || model.scope() == null || model.scope().repositoryId() == null) {
                throw new IllegalArgumentException("repository QIR must include scope.repositoryId");
            }
            String repositoryId = model.scope().repositoryId();
            if (!repositoryIds.add(repositoryId)) {
                throw new IllegalArgumentException("duplicate repository identity: " + repositoryId);
            }
            Map<String, String> ids = new HashMap<>();
            for (QirNode node : model.nodes()) {
                String globalId = node.id().equals(repositoryId)
                        ? repositoryId
                        : repositoryId + "::" + node.id();
                ids.put(node.id(), globalId);
                Map<String, Object> metadata = new HashMap<>();
                if (node.metadata() != null) metadata.putAll(node.metadata());
                metadata.put("repositoryId", repositoryId);
                metadata.put("system", systemName);
                nodes.add(new QirNode(globalId, node.type(), node.name(), node.technology(), Map.copyOf(metadata)));
            }

            edges.add(new QirEdge(systemId, repositoryId, "contains",
                    Map.of("scope", "repository")));
            for (QirEdge edge : model.edges()) {
                String from = ids.get(edge.from());
                String to = ids.get(edge.to());
                if (from == null || to == null) continue;
                Map<String, Object> metadata = new HashMap<>();
                if (edge.metadata() != null) metadata.putAll(edge.metadata());
                metadata.put("repositoryId", repositoryId);
                edges.add(new QirEdge(from, to, edge.relationship(), Map.copyOf(metadata)));
            }
            for (Finding finding : model.findings()) {
                String resourceId = ids.getOrDefault(finding.resourceId(), repositoryId + "::" + finding.resourceId());
                findings.add(new Finding(
                        repositoryId + "::" + finding.id(),
                        finding.severity(),
                        finding.category(),
                        resourceId,
                        finding.title(),
                        finding.recommendation()));
            }
        }

        edges.addAll(dependencyResolver.resolve(nodes));

        return new QirModel(systemName, "multi-repo", List.copyOf(nodes), List.copyOf(edges),
                List.copyOf(findings), new QirScope(systemName, systemId));
    }

    private String encode(String value) {
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
