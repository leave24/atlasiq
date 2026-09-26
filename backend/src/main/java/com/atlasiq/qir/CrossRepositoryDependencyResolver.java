package com.atlasiq.qir;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class CrossRepositoryDependencyResolver {

    public List<QirEdge> resolve(List<QirNode> nodes) {
        Map<String, List<QirNode>> targets = new HashMap<>();
        for (QirNode node : nodes) {
            if (isRepositoryOrSystem(node)) continue;
            index(targets, node.name(), node);
            Object serviceName = node.metadata() == null ? null : node.metadata().get("serviceName");
            if (serviceName != null) index(targets, serviceName.toString(), node);
        }

        List<QirEdge> resolved = new ArrayList<>();
        for (QirNode source : nodes) {
            if (isRepositoryOrSystem(source) || source.metadata() == null) continue;
            resolveValue(source, source.metadata().get("dependsOn"), "depends-on", "dependsOn", targets, resolved);
            resolveValue(source, source.metadata().get("targetService"), "calls", "targetService", targets, resolved);

            Object targetUrl = source.metadata().get("targetUrl");
            if (targetUrl != null) {
                String host = host(targetUrl.toString());
                if (host != null) resolveOne(source, host, "calls", "targetUrl", targets, resolved);
            }
        }
        return List.copyOf(resolved);
    }

    private void resolveValue(QirNode source, Object value, String relationship, String evidence,
                              Map<String, List<QirNode>> targets, List<QirEdge> resolved) {
        if (value instanceof Iterable<?> values) {
            for (Object item : values) if (item != null) resolveOne(source, item.toString(), relationship, evidence, targets, resolved);
        } else if (value != null) {
            resolveOne(source, value.toString(), relationship, evidence, targets, resolved);
        }
    }

    private void resolveOne(QirNode source, String targetName, String relationship, String evidence,
                            Map<String, List<QirNode>> targets, List<QirEdge> resolved) {
        List<QirNode> matches = targets.getOrDefault(normalize(targetName), List.of()).stream()
                .filter(target -> !repositoryId(source).equals(repositoryId(target)))
                .toList();
        if (matches.size() != 1) return;

        QirNode target = matches.getFirst();
        resolved.add(new QirEdge(source.id(), target.id(), relationship, Map.of(
                "scope", "cross-repository",
                "evidence", evidence,
                "target", targetName,
                "confidence", "explicit")));
    }

    private void index(Map<String, List<QirNode>> targets, String key, QirNode node) {
        if (key == null || key.isBlank()) return;
        targets.computeIfAbsent(normalize(key), ignored -> new ArrayList<>()).add(node);
    }

    private String repositoryId(QirNode node) {
        Object value = node.metadata() == null ? null : node.metadata().get("repositoryId");
        return value == null ? "" : value.toString();
    }

    private boolean isRepositoryOrSystem(QirNode node) {
        return "repository".equals(node.type()) || "system".equals(node.type());
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String host(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getHost();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
