package com.atlasiq.parser.kubernetes;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@ApplicationScoped
public class KubernetesParser {

    private static final Set<String> SUPPORTED = Set.of(
            "Deployment", "StatefulSet", "DaemonSet", "Service", "Ingress",
            "ConfigMap", "Secret", "ServiceAccount", "NetworkPolicy", "HorizontalPodAutoscaler", "PodDisruptionBudget");

    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public KubernetesAnalysis parse(Path root) {
        var resources = new ArrayList<Resource>();
        var nodes = new ArrayList<QirNode>();
        var edges = new ArrayList<QirEdge>();
        var findings = new ArrayList<Finding>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isYaml)
                    .forEach(path -> parseFile(root, path, resources, nodes, findings));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan Kubernetes manifests under " + root, e);
        }

        resolve(resources, edges);
        return new KubernetesAnalysis(List.copyOf(nodes), List.copyOf(edges), List.copyOf(findings));
    }

    private void parseFile(Path root, Path path, List<Resource> resources, List<QirNode> nodes, List<Finding> findings) {
        try {
            Iterator<JsonNode> documents = yaml.readerFor(JsonNode.class).readValues(Files.newInputStream(path));
            while (documents.hasNext()) {
                JsonNode doc = documents.next();
                String kind = text(doc, "kind");
                String name = text(doc.path("metadata"), "name");
                if (!SUPPORTED.contains(kind) || name == null) continue;

                String namespace = text(doc.path("metadata"), "namespace");
                if (namespace == null) namespace = "default";
                String id = id(kind, namespace, name);
                String source = root.relativize(path).toString().replace('\\', '/');
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("namespace", namespace);
                metadata.put("source", source);
                metadata.put("apiVersion", text(doc, "apiVersion"));

                Resource resource = new Resource(id, kind, namespace, name, doc, source);
                resources.add(resource);
                nodes.add(new QirNode(id, kubernetesType(kind), name, "kubernetes", Map.copyOf(metadata)));

                if (isWorkload(kind)) inspectWorkload(resource, findings);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse YAML file " + path, e);
        }
    }

    private void resolve(List<Resource> resources, List<QirEdge> edges) {
        Map<String, Resource> byIdentity = new HashMap<>();
        for (Resource r : resources) byIdentity.put(r.namespace + "/" + r.kind + "/" + r.name, r);

        for (Resource r : resources) {
            if ("Ingress".equals(r.kind)) {
                for (JsonNode rule : r.document.path("spec").path("rules")) {
                    for (JsonNode path : rule.path("http").path("paths")) {
                        String service = text(path.path("backend").path("service"), "name");
                        Resource target = byIdentity.get(r.namespace + "/Service/" + service);
                        if (target != null) edges.add(edge(r, target, "routes_to"));
                    }
                }
            }
            if ("Service".equals(r.kind)) {
                Map<String, String> selector = stringMap(r.document.path("spec").path("selector"));
                if (!selector.isEmpty()) {
                    resources.stream()
                            .filter(w -> isWorkload(w.kind) && w.namespace.equals(r.namespace))
                            .filter(w -> labelsMatch(selector, stringMap(podTemplate(w).path("metadata").path("labels"))))
                            .forEach(w -> edges.add(edge(r, w, "selects")));
                }
            }
            if (isWorkload(r.kind)) resolveWorkloadRefs(r, byIdentity, edges);
        }
    }

    private void resolveWorkloadRefs(Resource workload, Map<String, Resource> byIdentity, List<QirEdge> edges) {
        JsonNode pod = podTemplate(workload).path("spec");
        String serviceAccount = text(pod, "serviceAccountName");
        addRef(workload, byIdentity, edges, "ServiceAccount", serviceAccount, "uses_service_account");

        for (JsonNode container : pod.path("containers")) {
            for (JsonNode envFrom : container.path("envFrom")) {
                addRef(workload, byIdentity, edges, "ConfigMap", text(envFrom.path("configMapRef"), "name"), "reads_config");
                addRef(workload, byIdentity, edges, "Secret", text(envFrom.path("secretRef"), "name"), "reads_secret");
            }
            for (JsonNode env : container.path("env")) {
                JsonNode valueFrom = env.path("valueFrom");
                addRef(workload, byIdentity, edges, "ConfigMap", text(valueFrom.path("configMapKeyRef"), "name"), "reads_config");
                addRef(workload, byIdentity, edges, "Secret", text(valueFrom.path("secretKeyRef"), "name"), "reads_secret");
            }
        }
        for (JsonNode volume : pod.path("volumes")) {
            addRef(workload, byIdentity, edges, "ConfigMap", text(volume.path("configMap"), "name"), "mounts_config");
            addRef(workload, byIdentity, edges, "Secret", text(volume.path("secret"), "secretName"), "mounts_secret");
        }
    }

    private void inspectWorkload(Resource r, List<Finding> findings) {
        JsonNode pod = podTemplate(r).path("spec");
        if (!pod.path("securityContext").path("runAsNonRoot").asBoolean(false)) {
            findings.add(finding(r, "MEDIUM", "security", "Pod securityContext does not enforce runAsNonRoot", "Set spec.template.spec.securityContext.runAsNonRoot: true."));
        }
        if (r.document.path("spec").path("template").path("spec").path("hostNetwork").asBoolean(false)) {
            findings.add(finding(r, "HIGH", "security", "Workload uses hostNetwork", "Avoid hostNetwork unless it is strictly required."));
        }
        for (JsonNode volume : pod.path("volumes")) {
            if (!volume.path("hostPath").isMissingNode()) {
                findings.add(finding(r, "HIGH", "security", "Workload mounts hostPath", "Replace hostPath with a safer volume type when possible."));
            }
        }
        for (JsonNode container : pod.path("containers")) {
            String containerName = text(container, "name");
            JsonNode resources = container.path("resources");
            if (resources.path("requests").isMissingNode() || resources.path("limits").isMissingNode()) {
                findings.add(finding(r, "MEDIUM", "reliability", "Container " + containerName + " has incomplete resource requests/limits", "Define CPU and memory requests and limits."));
            }
            if (container.path("readinessProbe").isMissingNode()) {
                findings.add(finding(r, "MEDIUM", "reliability", "Container " + containerName + " has no readinessProbe", "Add a readiness probe."));
            }
            if (container.path("livenessProbe").isMissingNode()) {
                findings.add(finding(r, "LOW", "reliability", "Container " + containerName + " has no livenessProbe", "Add a liveness probe."));
            }
            if (container.path("securityContext").path("privileged").asBoolean(false)) {
                findings.add(finding(r, "CRITICAL", "security", "Container " + containerName + " is privileged", "Remove privileged mode."));
            }
        }
    }

    private JsonNode podTemplate(Resource r) { return r.document.path("spec").path("template"); }
    private boolean isWorkload(String kind) { return Set.of("Deployment", "StatefulSet", "DaemonSet").contains(kind); }
    private boolean isYaml(Path path) { String n = path.getFileName().toString().toLowerCase(); return n.endsWith(".yaml") || n.endsWith(".yml"); }
    private String kubernetesType(String kind) { return "kubernetes-" + kind.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(); }
    private String id(String kind, String namespace, String name) { return "k8s:" + namespace + ":" + kind.toLowerCase() + ":" + name; }
    private QirEdge edge(Resource from, Resource to, String rel) { return new QirEdge(from.id, to.id, rel, Map.of("source", from.source)); }
    private Finding finding(Resource r, String severity, String category, String title, String recommendation) { return new Finding(r.id + ":" + Integer.toHexString(title.hashCode()), severity, category, r.id, title, recommendation); }

    private void addRef(Resource from, Map<String, Resource> resources, List<QirEdge> edges, String kind, String name, String rel) {
        if (name == null) return;
        Resource target = resources.get(from.namespace + "/" + kind + "/" + name);
        if (target != null) edges.add(edge(from, target, rel));
    }

    private boolean labelsMatch(Map<String, String> selector, Map<String, String> labels) {
        return !selector.isEmpty() && selector.entrySet().stream().allMatch(e -> e.getValue().equals(labels.get(e.getKey())));
    }

    private Map<String, String> stringMap(JsonNode node) {
        Map<String, String> result = new HashMap<>();
        if (node != null && node.isObject()) node.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText()));
        return result;
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isMissingNode() || node.path(field).isNull()) return null;
        String value = node.path(field).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private record Resource(String id, String kind, String namespace, String name, JsonNode document, String source) {}
}
