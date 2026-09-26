package com.atlasiq.qir;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossRepositoryDependencyResolverTest {

    private final CrossRepositoryDependencyResolver resolver = new CrossRepositoryDependencyResolver();

    @Test
    void resolvesExplicitDependencyAcrossRepositories() {
        QirNode frontend = node("repo:web::frontend", "frontend", "repo:web",
                Map.of("dependsOn", "orders-api"));
        QirNode api = node("repo:api::service", "orders-api", "repo:api",
                Map.of("serviceName", "orders-api"));

        var edges = resolver.resolve(List.of(frontend, api));

        assertEquals(1, edges.size());
        assertEquals(frontend.id(), edges.getFirst().from());
        assertEquals(api.id(), edges.getFirst().to());
        assertEquals("depends-on", edges.getFirst().relationship());
        assertEquals("explicit", edges.getFirst().metadata().get("confidence"));
    }

    @Test
    void resolvesTargetUrlHostAcrossRepositories() {
        QirNode client = node("repo:web::client", "web-client", "repo:web",
                Map.of("targetUrl", "https://payments.internal/api"));
        QirNode service = node("repo:payments::service", "payments.internal", "repo:payments", Map.of());

        var edges = resolver.resolve(List.of(client, service));

        assertEquals(1, edges.size());
        assertEquals("calls", edges.getFirst().relationship());
        assertEquals("targetUrl", edges.getFirst().metadata().get("evidence"));
    }

    @Test
    void resolvesSanitizedDependencyHintAndCarriesSourceEvidence() {
        QirNode hint = node("repo:web::dependency:config", "application.properties", "repo:web",
                Map.of("targetHost", "orders_internal", "targetUrl", "https://orders_internal:8443",
                        "evidenceFile", "application.properties", "evidenceKind", "declared-url"));
        QirNode service = node("repo:orders::service", "orders-service", "repo:orders",
                Map.of("hostname", "orders_internal"));

        var edges = resolver.resolve(List.of(hint, service));

        assertEquals(1, edges.size());
        assertEquals("calls", edges.getFirst().relationship());
        assertEquals("targetHost", edges.getFirst().metadata().get("evidence"));
        assertEquals("application.properties", edges.getFirst().metadata().get("evidenceFile"));
        assertEquals("declared-url", edges.getFirst().metadata().get("evidenceKind"));
    }

    @Test
    void resolvesLegacyTargetUrlWithInternalUnderscoreHost() {
        QirNode hint = node("repo:web::dependency:config", "application.properties", "repo:web",
                Map.of("targetUrl", "http://orders_internal:8080/api"));
        QirNode service = node("repo:orders::service", "orders-service", "repo:orders",
                Map.of("host", "orders_internal"));

        var edges = resolver.resolve(List.of(hint, service));

        assertEquals(1, edges.size());
        assertEquals(service.id(), edges.getFirst().to());
    }

    @Test
    void deduplicatesEquivalentExplicitEvidence() {
        QirNode source = node("repo:web::client", "client", "repo:web",
                Map.of("targetHost", "orders.internal", "targetService", "orders.internal"));
        QirNode target = node("repo:orders::service", "orders.internal", "repo:orders", Map.of());

        var edges = resolver.resolve(List.of(source, target));

        assertEquals(1, edges.size());
        assertEquals("calls", edges.getFirst().relationship());
    }

    @Test
    void ignoresAmbiguousTargetsInsteadOfGuessing() {
        QirNode source = node("repo:web::client", "client", "repo:web", Map.of("targetService", "api"));
        QirNode first = node("repo:a::api", "api", "repo:a", Map.of());
        QirNode second = node("repo:b::api", "api", "repo:b", Map.of());

        assertTrue(resolver.resolve(List.of(source, first, second)).isEmpty());
    }

    @Test
    void doesNotCreateCrossRepoEdgeWithinSameRepository() {
        QirNode source = node("repo:a::client", "client", "repo:a", Map.of("dependsOn", "api"));
        QirNode target = node("repo:a::api", "api", "repo:a", Map.of());

        assertTrue(resolver.resolve(List.of(source, target)).isEmpty());
    }

    private QirNode node(String id, String name, String repositoryId, Map<String, Object> extra) {
        var metadata = new java.util.HashMap<String, Object>(extra);
        metadata.put("repositoryId", repositoryId);
        return new QirNode(id, "component", name, "test", Map.copyOf(metadata));
    }
}
