package com.atlasiq.parser.githubactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubActionsParserTest {

    @TempDir
    Path root;

    @Test
    void parsesWorkflowJobsDependenciesAndActions() throws Exception {
        Path workflows = Files.createDirectories(root.resolve(".github/workflows"));
        Files.writeString(workflows.resolve("ci.yml"), """
                name: CI
                on:
                  push:
                  pull_request:
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@v4
                      - name: Build
                        run: ./mvnw test
                  deploy:
                    needs: build
                    runs-on: ubuntu-latest
                    steps:
                      - uses: docker/login-action@9780b0c442fbb1117ed29e0efdff1e18412f7567
                """);

        var result = new GitHubActionsParser().parse(root);

        assertTrue(result.nodes().stream().anyMatch(node -> node.type().equals("ci-workflow")));
        assertTrue(result.nodes().stream().filter(node -> node.type().equals("ci-job")).count() == 2);
        assertTrue(result.nodes().stream().anyMatch(node -> node.type().equals("ci-action")));
        assertTrue(result.edges().stream().anyMatch(edge -> edge.relationship().equals("precedes")));
        assertTrue(result.edges().stream().anyMatch(edge -> edge.relationship().equals("uses")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("does not declare explicit permissions")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("actions/checkout@v4")));
    }

    @Test
    void acceptsExplicitPermissionsAndShaPinnedAction() throws Exception {
        Path workflows = Files.createDirectories(root.resolve(".github/workflows"));
        Files.writeString(workflows.resolve("secure.yaml"), """
                name: Secure CI
                on: push
                permissions:
                  contents: read
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
                """);

        var result = new GitHubActionsParser().parse(root);

        assertTrue(result.findings().isEmpty(), () -> "Unexpected findings: " + result.findings());
    }
}
