package com.atlasiq.qir;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@ApplicationScoped
public class CrossRepositoryDependencyResolver {

    public List<QirEdge> resolve(List<QirNode> nodes) {
        Map<String, List<QirNode>> targets = new HashMap<>();
        for (QirNode node : nodes) {
            if (isRepositoryOrSystem(node)) continue;
            index(targets, node.name(), node);
            if (node.metadata() != null) {
                indexMetadataAlias(targets, node, "serviceName");
                indexMetadataAlias(targets, node, "hostname");
                indexMetadataAlias(targets, node, "host");
            }
        }

        List<QirEdge> resolved = new ArrayList<>();
        for (QirNode source : nodes) {
            if (isRepositoryOrSystem(source) || source.metadata() == null) continue;
            resolveValue(source, source.metadata().get("dependsOn"), "depends-on", "dependsOn", targets, resolved);
            resolveValue(source, source.metadata().get("targetService"), "calls", "targetService", targets, resolved);

            Object targetHost = source.metadata().get("targetHost");
            if (targetHost != null) {
                resolveOne(source, targetHost.toString(), "calls", "targetHost", targets, resolved);
            } else {
                Object targetUrl = source.metadata().get("targetUrl");
                if (targetUrl != null) {
                    String host = host(targetUrl.toString());
                    if (host != null) resolveOne(source, host, "calls", "targetUrl", targets, resolved);
                }
            }
        }
        Set<String> seen = new HashSet<>();
        return resolved.stream()
                .filter(edge -> seen.add(edge.from() + "\u0000" + edge.to() + "\u0000" + edge.relationship()))
                .toList();
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
        var metadata = new HashMap<String, Object>();
        metadata.put("scope", "cross-repository");
        metadata.put("evidence", evidence);
        metadata.put("target", targetName);
        metadata.put("confidence", "explicit");
        copyEvidence(source, metadata, "evidenceFile");
        copyEvidence(source, metadata, "evidenceKind");
        resolved.add(new QirEdge(source.id(), target.id(), relationship, Map.copyOf(metadata)));
    }

    private void indexMetadataAlias(Map<String, List<QirNode>> targets, QirNode node, String key) {
        Object value = node.metadata().get(key);
        if (value != null) index(targets, value.toString(), node);
    }

    private void copyEvidence(QirNode source, Map<String, Object> target, String key) {
        Object value = source.metadata().get(key);
        if (value != null) target.put(key, value);
    }

    private void index(Map<String, List<QirNode>> targets, String key, QirNode node) {
        if (key == null || key.isBlank()) return;
        List<QirNode> bucket = targets.computeIfAbsent(normalize(key), ignored -> new ArrayList<>());
        if (bucket.stream().noneMatch(existing -> existing.id().equals(node.id()))) {
            bucket.add(node);
        }
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
        int schemeEnd = value.indexOf("://");
        if (schemeEnd <= 0) return null;
        int start = schemeEnd + 3;
        int end = value.length();
        for (int i = start; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '/' || ch == '?' || ch == '#') {
                end = i;
                break;
            }
        }
        String authority = value.substring(start, end);
        int at = authority.lastIndexOf('@');
        if (at >= 0) authority = authority.substring(at + 1);
        if (authority.startsWith("[")) {
            int closing = authority.indexOf(']');
            return closing >= 0 ? authority.substring(0, closing + 1) : null;
        }
        int colon = authority.lastIndexOf(':');
        if (colon > 0 && authority.substring(colon + 1).chars().allMatch(Character::isDigit)) {
            authority = authority.substring(0, colon);
        }
        return authority.isBlank() ? null : authority;
    }
}
