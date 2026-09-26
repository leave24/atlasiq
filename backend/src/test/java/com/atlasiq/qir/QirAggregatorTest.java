package com.atlasiq.qir;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QirAggregatorTest {

    private final QirAggregator aggregator = new QirAggregator(new CrossRepositoryDependencyResolver());

    @Test
    void aggregatesRepositoriesUnderSystemAndNamespacesComponentIds() {
        QirModel api = repository("repo:github:acme/api", "api", "service");
        QirModel worker = repository("repo:github:acme/worker", "worker", "service");

        QirModel result = aggregator.aggregate("commerce", List.of(api, worker));

        assertEquals("commerce", result.scope().system());
        assertTrue(result.scope().repositoryId().startsWith("system:"));
        assertEquals(5, result.nodes().size());
        assertTrue(result.nodes().stream().anyMatch(n -> n.id().equals("repo:github:acme/api::service")));
        assertTrue(result.nodes().stream().anyMatch(n -> n.id().equals("repo:github:acme/worker::service")));
        assertTrue(result.edges().stream().anyMatch(e -> e.from().equals(result.scope().repositoryId())
                && e.to().equals("repo:github:acme/api") && e.relationship().equals("contains")));
    }

    @Test
    void rewritesEdgesAndFindingsToGlobalIds() {
        String repositoryId = "repo:github:acme/api";
        QirModel model = new QirModel(
                "acme/api",
                "main",
                List.of(
                        new QirNode(repositoryId, "repository", "api", "github", Map.of()),
                        new QirNode("service", "kubernetes-service", "api", "kubernetes", Map.of()),
                        new QirNode("deployment", "kubernetes-deployment", "api", "kubernetes", Map.of())),
                List.of(new QirEdge("service", "deployment", "selects", Map.of())),
                List.of(new Finding("finding-1", "HIGH", "security", "deployment", "Example", "Fix it")),
                new QirScope("commerce", repositoryId));

        QirModel result = aggregator.aggregate("commerce", List.of(model));

        assertTrue(result.edges().stream().anyMatch(e ->
                e.from().equals(repositoryId + "::service") && e.to().equals(repositoryId + "::deployment")));
        assertEquals(repositoryId + "::deployment", result.findings().getFirst().resourceId());
        assertEquals(repositoryId + "::finding-1", result.findings().getFirst().id());
    }

    @Test
    void sameLocalComponentIdInDifferentRepositoriesDoesNotCollide() {
        QirModel first = repository("repo:local:first", "first", "k8s:default:service:api");
        QirModel second = repository("repo:local:second", "second", "k8s:default:service:api");

        QirModel result = aggregator.aggregate("platform", List.of(first, second));

        var componentIds = result.nodes().stream()
                .filter(n -> n.type().equals("component"))
                .map(QirNode::id)
                .toList();
        assertEquals(2, componentIds.size());
        assertNotEquals(componentIds.get(0), componentIds.get(1));
    }

    @Test
    void rejectsDuplicateRepositoryIdentity() {
        QirModel first = repository("repo:github:acme/api", "api-main", "service");
        QirModel second = repository("repo:github:acme/api", "api-other-ref", "service");

        var error = assertThrows(IllegalArgumentException.class,
                () -> aggregator.aggregate("commerce", List.of(first, second)));

        assertTrue(error.getMessage().contains("duplicate repository identity"));
    }

    private QirModel repository(String repositoryId, String name, String componentId) {
        return new QirModel(
                name,
                "main",
                List.of(
                        new QirNode(repositoryId, "repository", name, "github", Map.of()),
                        new QirNode(componentId, "component", componentId, "test", Map.of())),
                List.of(new QirEdge(repositoryId, componentId, "contains", Map.of())),
                List.of(),
                new QirScope("default", repositoryId));
    }
}
