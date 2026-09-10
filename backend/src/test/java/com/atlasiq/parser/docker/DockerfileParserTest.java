package com.atlasiq.parser.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerfileParserTest {

    @TempDir
    Path root;

    @Test
    void detectsRootUnpinnedBaseAndEmbeddedSecret() throws Exception {
        Files.writeString(root.resolve("Dockerfile"), """
                FROM eclipse-temurin:latest
                ENV API_TOKEN=hardcoded-value
                USER root
                """);

        var result = new DockerfileParser().parse(root);

        assertTrue(result.nodes().stream().anyMatch(node -> node.type().equals("container-image")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("not pinned")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("Possible secret")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("runs as root")));
    }

    @Test
    void acceptsPinnedNonRootDockerfile() throws Exception {
        Files.writeString(root.resolve("Dockerfile.backend"), """
                FROM eclipse-temurin@sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
                USER 10001
                """);

        var result = new DockerfileParser().parse(root);
        assertTrue(result.findings().isEmpty(), () -> "Unexpected findings: " + result.findings());
    }
}
