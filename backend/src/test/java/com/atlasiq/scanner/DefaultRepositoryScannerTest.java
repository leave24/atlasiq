package com.atlasiq.scanner;

import com.atlasiq.parser.docker.DockerfileParser;
import com.atlasiq.parser.dependencies.DependencyHintParser;
import com.atlasiq.parser.githubactions.GitHubActionsParser;
import com.atlasiq.parser.kubernetes.KubernetesParser;
import com.atlasiq.parser.terraform.TerraformParser;
import com.atlasiq.parser.api.ApiDiscoveryParser;
import com.atlasiq.parser.api.HttpClientDiscoveryParser;
import com.atlasiq.qir.ApiConsumerCorrelator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRepositoryScannerTest {

    @TempDir
    Path workspace;

    @Test
    void scansRepositoryInsideWorkspaceAndAggregatesQir() throws Exception {
        Path repository = Files.createDirectories(workspace.resolve("demo"));
        Files.writeString(repository.resolve("app.yaml"), """
                apiVersion: v1
                kind: Service
                metadata:
                  name: api
                  namespace: demo
                spec:
                  selector:
                    app: api
                  ports:
                    - port: 8080
                ---
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: api
                  namespace: demo
                spec:
                  selector:
                    matchLabels: { app: api }
                  template:
                    metadata:
                      labels: { app: api }
                    spec:
                      securityContext:
                        runAsNonRoot: true
                      containers:
                        - name: api
                          image: api:local
                          resources:
                            requests: { cpu: 100m, memory: 64Mi }
                            limits: { cpu: 500m, memory: 256Mi }
                          readinessProbe:
                            tcpSocket: { port: 8080 }
                          livenessProbe:
                            tcpSocket: { port: 8080 }
                """);
        Files.writeString(repository.resolve("Dockerfile"), """
                FROM eclipse-temurin:21-jre
                USER 10001
                COPY target/app.jar /app/app.jar
                """);
        Path workflows = Files.createDirectories(repository.resolve(".github/workflows"));
        Files.writeString(workflows.resolve("ci.yml"), """
                name: CI
                on: push
                permissions:
                  contents: read
                jobs:
                  build:
                    runs-on: ubuntu-latest
                    steps:
                      - uses: actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683
                """);

        var scanner = scanner();
        var qir = scanner.scan(new ScanRequest("demo", "local"));

        assertTrue(qir.nodes().stream().anyMatch(node -> node.type().equals("repository")));
        assertTrue(qir.nodes().stream().anyMatch(node -> node.id().equals(qir.scope().repositoryId())));
        assertTrue(qir.scope().repositoryId().startsWith("repo:local:"));
        assertTrue(qir.scope().system().equals("default"));
        assertTrue(qir.nodes().stream().anyMatch(node -> node.type().equals("kubernetes-deployment")));
        assertTrue(qir.nodes().stream().anyMatch(node -> node.type().equals("container-image")));
        assertTrue(qir.nodes().stream().anyMatch(node -> node.type().equals("ci-workflow")));
        assertTrue(qir.edges().stream().anyMatch(edge -> edge.relationship().equals("selects")));
        assertTrue(qir.edges().stream().anyMatch(edge -> edge.relationship().equals("uses")));
        assertTrue(qir.edges().stream().anyMatch(edge -> edge.relationship().equals("contains")));
    }

    @Test
    void preservesExplicitSystemScope() throws Exception {
        Files.createDirectories(workspace.resolve("payments"));

        var qir = scanner().scan(new ScanRequest("payments", "local", "commerce"));

        assertTrue(qir.scope().system().equals("commerce"));
        assertTrue(qir.scope().repositoryId().startsWith("repo:local:"));
        assertTrue(qir.edges().stream().noneMatch(edge -> edge.from().equals("repository")));
    }

    @Test
    void keepsDistinctLocalRepositoryPathsCollisionFree() throws Exception {
        Files.createDirectories(workspace.resolve("payments@v1"));
        Files.createDirectories(workspace.resolve("payments-v1"));

        var first = scanner().scan(new ScanRequest("payments@v1", "local"));
        var second = scanner().scan(new ScanRequest("payments-v1", "local"));

        assertTrue(!first.scope().repositoryId().equals(second.scope().repositoryId()));
    }

    @Test
    void rejectsPathTraversalOutsideWorkspace() {
        assertThrows(IllegalArgumentException.class,
                () -> scanner().scan(new ScanRequest("../outside", "local")));
    }

    private DefaultRepositoryScanner scanner() {
        return new DefaultRepositoryScanner(
                new KubernetesParser(),
                new DockerfileParser(),
                new GitHubActionsParser(),
                new TerraformParser(),
                new DependencyHintParser(),
                new ApiDiscoveryParser(),
                new HttpClientDiscoveryParser(),
                new ApiConsumerCorrelator(),
                new GitHubRepositoryAcquirer(workspace.toString()),
                workspace.toString());
    }
}
