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
        assertEquals("payments.internal", nodes.getFirst().metadata().get("targetHost"));
    }

    @Test
    void stripsPathsQueriesAndFragmentsFromEvidence() throws Exception {
        Files.writeString(repository.resolve(".env"),
                "API=https://api.example:8443/private?token=secret#docs");

        var nodes = new DependencyHintParser().parse(repository);

        assertEquals(1, nodes.size());
        assertEquals("https://api.example:8443", nodes.getFirst().metadata().get("targetUrl"));
        assertEquals("api.example", nodes.getFirst().metadata().get("targetHost"));
    }

    @Test
    void ignoresUrlsWithCredentials() throws Exception {
        Files.writeString(repository.resolve(".env"),
                "API=******api.example:8443/private?token=secret");

        assertTrue(new DependencyHintParser().parse(repository).isEmpty());
    }

    @Test
    void preservesInternalHostsWithUnderscores() throws Exception {
        Files.writeString(repository.resolve("application.properties"),
                "service.url=http://my_service:8080/api");

        var nodes = new DependencyHintParser().parse(repository);

        assertEquals(1, nodes.size());
        assertEquals("http://my_service:8080", nodes.getFirst().metadata().get("targetUrl"));
        assertEquals("my_service", nodes.getFirst().metadata().get("targetHost"));
    }

    @Test
    void preservesBracketedIpv6Hosts() throws Exception {
        Files.writeString(repository.resolve("application.properties"),
                "service.url=https://[2001:db8::1]:8443/api");

        var nodes = new DependencyHintParser().parse(repository);

        assertEquals(1, nodes.size());
        assertEquals("https://[2001:db8::1]:8443", nodes.getFirst().metadata().get("targetUrl"));
    }

    @Test
    void discoversUrlsWithQueryOrFragmentWithoutPath() throws Exception {
        Files.writeString(repository.resolve(".env"),
                "QUERY=https://service.internal?token=secret\nFRAGMENT=https://service.internal#docs\n");

        var nodes = new DependencyHintParser().parse(repository);

        assertEquals(2, nodes.size());
        assertTrue(nodes.stream().allMatch(node -> "https://service.internal".equals(node.metadata().get("targetUrl"))));
    }

    @Test
    void ignoresMalformedAuthorities() throws Exception {
        Files.writeString(repository.resolve(".env"),
                "BROKEN_ONE=https://-bad-host/api\nBROKEN_TWO=https://.bad-host/api\nBROKEN_THREE=https://bad-.host/api\nBROKEN_FOUR=https://foo.-bar/api\n");

        assertTrue(new DependencyHintParser().parse(repository).isEmpty());
    }

    @Test
    void ignoresDirectorySymlinkCycles() throws Exception {
        Path nested = Files.createDirectories(repository.resolve("nested"));
        try {
            Files.createSymbolicLink(nested.resolve("loop"), repository);
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
            return;
        }
        Files.writeString(repository.resolve("application.properties"), "api=https://safe.internal/api");

        var nodes = new DependencyHintParser().parse(repository);

        assertEquals(1, nodes.size());
        assertEquals("https://safe.internal", nodes.getFirst().metadata().get("targetUrl"));
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
