package com.atlasiq.parser.dependencies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyHintParserTest {

    // Security regressions here intentionally exercise untrusted repository inputs.

    @TempDir
    Path repository;

    @Test
    void discoversServiceUrlsFromSupportedConfiguration() throws Exception {
        Files.writeString(repository.resolve("application.properties"),
                "payments.url=https://payments.internal/api\nother=value\n");

        var nodes = new DependencyHintParser().parse(repository);

        assertEquals(1, nodes.size());
        assertEquals("dependency-reference", nodes.getFirst().type());
        assertEquals("https://payments.internal", nodes.getFirst().metadata().get("targetUrl"));
        assertEquals("application.properties", nodes.getFirst().metadata().get("evidenceFile"));
    }

    @Test
    void stripsPathsQueriesAndCredentialsFromEvidence() throws Exception {
        Files.writeString(repository.resolve(".env"),
                "API=https://user:pass@api.example:8443/private?token=secret");

        var nodes = new DependencyHintParser().parse(repository);

        assertEquals(1, nodes.size());
        assertEquals("https://api.example:8443", nodes.getFirst().metadata().get("targetUrl"));
    }

    @Test
    void ignoresGeneratedDirectories() throws Exception {
        Path generated = Files.createDirectories(repository.resolve("target/classes"));
        Files.writeString(generated.resolve("application.properties"), "api=https://generated.internal/api");

        assertTrue(new DependencyHintParser().parse(repository).isEmpty());
    }

    @Test
    void ignoresSymlinkedConfigurationFiles() throws Exception {
        Path outside = Files.createTempFile("atlasiq-secret", ".env");
        Files.writeString(outside, "api=https://secret.internal?token=hidden");
        Path link = repository.resolve(".env");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
            return;
        }

        assertTrue(new DependencyHintParser().parse(repository).isEmpty());
    }

    @Test
    void ignoresUrlsInUnsupportedSourceFiles() throws Exception {
        Files.writeString(repository.resolve("README.md"), "https://payments.internal/api");

        assertTrue(new DependencyHintParser().parse(repository).isEmpty());
    }

    @Test
    void discoversMultipleDeclaredDependenciesWithoutGuessing() throws Exception {
        Files.writeString(repository.resolve(".env"),
                "ORDERS_URL=http://orders.internal:8080/api\nPAYMENTS_URL=https://payments.internal/v1\n");

        var nodes = new DependencyHintParser().parse(repository);

        assertEquals(2, nodes.size());
        assertTrue(nodes.stream().allMatch(node -> "declared-url".equals(node.metadata().get("evidenceKind"))));
    }
}
