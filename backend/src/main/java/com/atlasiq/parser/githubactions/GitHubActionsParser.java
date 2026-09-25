package com.atlasiq.parser.githubactions;

import com.atlasiq.qir.Finding;
import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@ApplicationScoped
public class GitHubActionsParser {

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public GitHubActionsAnalysis parse(Path root) {
        Path workflows = root.resolve(".github/workflows");
        if (!Files.isDirectory(workflows)) {
            return new GitHubActionsAnalysis(List.of(), List.of(), List.of());
        }

        var nodes = new ArrayList<QirNode>();
        var edges = new ArrayList<QirEdge>();
        var findings = new ArrayList<Finding>();
        var actions = new HashSet<String>();

        try (Stream<Path> paths = Files.walk(workflows)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isWorkflowFile)
                    .forEach(path -> parseFile(root, path, nodes, edges, findings, actions));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan GitHub Actions workflows under " + workflows, e);
        }

        return new GitHubActionsAnalysis(List.copyOf(nodes), List.copyOf(edges), List.copyOf(findings));
    }

    private void parseFile(
            Path root,
            Path path,
            List<QirNode> nodes,
            List<QirEdge> edges,
            List<Finding> findings,
            Set<String> actions) {
        try {
            JsonNode document = yaml.readTree(path.toFile());
            String source = root.relativize(path).toString().replace('\\', '/');
            String workflowId = "gha:" + source;
            String workflowName = text(document.get("name"), path.getFileName().toString());

            Map<String, Object> workflowMetadata = new HashMap<>();
            workflowMetadata.put("source", source);
            workflowMetadata.put("triggers", triggerNames(document.get("on")));
            nodes.add(new QirNode(workflowId, "ci-workflow", workflowName, "github-actions", Map.copyOf(workflowMetadata)));

            if (!document.has("permissions")) {
                findings.add(finding(workflowId, source, "MEDIUM", "least-privilege",
                        "Workflow does not declare explicit permissions",
                        "Declare the minimum required GitHub token permissions at workflow or job level."));
            }

            JsonNode jobs = document.get("jobs");
            if (jobs == null || !jobs.isObject()) {
                return;
            }

            jobs.fields().forEachRemaining(entry -> {
                String jobName = entry.getKey();
                JsonNode job = entry.getValue();
                String jobId = workflowId + ":job:" + jobName;

                Map<String, Object> jobMetadata = new HashMap<>();
                jobMetadata.put("source", source);
                jobMetadata.put("runsOn", text(job.get("runs-on"), "unspecified"));
                jobMetadata.put("needs", stringValues(job.get("needs")));
                nodes.add(new QirNode(jobId, "ci-job", jobName, "github-actions", Map.copyOf(jobMetadata)));
                edges.add(new QirEdge(workflowId, jobId, "contains", Map.of("source", source)));

                for (String dependency : stringValues(job.get("needs"))) {
                    edges.add(new QirEdge(workflowId + ":job:" + dependency, jobId, "precedes", Map.of("source", source)));
                }

                JsonNode steps = job.get("steps");
                if (steps == null || !steps.isArray()) {
                    return;
                }

                for (JsonNode step : steps) {
                    String uses = text(step.get("uses"), null);
                    if (uses == null || uses.isBlank()) {
                        continue;
                    }

                    String actionId = "gha-action:" + uses;
                    if (actions.add(actionId)) {
                        nodes.add(new QirNode(actionId, "ci-action", uses, "github-actions", Map.of("uses", uses)));
                    }
                    edges.add(new QirEdge(jobId, actionId, "uses", Map.of("source", source)));

                    if (!isPinnedToCommit(uses)) {
                        findings.add(finding(jobId, source, "MEDIUM", "supply-chain",
                                "Action is not pinned to a commit SHA: " + uses,
                                "Pin third-party actions to an immutable full commit SHA."));
                    }
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse GitHub Actions workflow " + path, e);
        }
    }

    private boolean isWorkflowFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private boolean isPinnedToCommit(String uses) {
        int at = uses.lastIndexOf('@');
        if (at < 0 || at == uses.length() - 1) {
            return false;
        }
        String ref = uses.substring(at + 1);
        return ref.matches("[0-9a-fA-F]{40}");
    }

    private List<String> triggerNames(JsonNode on) {
        if (on == null || on.isNull()) return List.of();
        if (on.isTextual()) return List.of(on.asText());
        if (on.isArray()) return stringValues(on);
        if (on.isObject()) {
            var result = new ArrayList<String>();
            on.fieldNames().forEachRemaining(result::add);
            return List.copyOf(result);
        }
        return List.of(on.asText());
    }

    private List<String> stringValues(JsonNode node) {
        if (node == null || node.isNull()) return List.of();
        if (node.isTextual()) return List.of(node.asText());
        if (node.isArray()) {
            var values = new ArrayList<String>();
            node.forEach(item -> values.add(item.asText()));
            return List.copyOf(values);
        }
        return List.of(node.asText());
    }

    private String text(JsonNode node, String fallback) {
        return node == null || node.isNull() ? fallback : node.asText();
    }

    private Finding finding(String resourceId, String source, String severity, String category, String title, String recommendation) {
        return new Finding(resourceId + ":" + Integer.toHexString(title.hashCode()), severity, category, resourceId,
                title + " [" + source + "]", recommendation);
    }
}
