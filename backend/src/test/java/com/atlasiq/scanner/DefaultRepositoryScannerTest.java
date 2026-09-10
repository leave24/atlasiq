package com.atlasiq.scanner;

import com.atlasiq.parser.docker.DockerfileParser;
import com.atlasiq.parser.kubernetes.KubernetesParser;
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

        var scanner = scanner();
        var qir = scanner.scan(new ScanRequest("demo", "local"));

        assertTrue(qir.nodes().stream().anyMatch(node -> node.type().equals("repository")));
        assertTrue(qir.nodes().stream().anyMatch(node -> node.type().equals("kubernetes-deployment")));
        assertTrue(qir.nodes().stream().anyMatch(node -> node.type().equals("container-image")));
        assertTrue(qir.edges().stream().anyMatch(edge -> edge.relationship().equals("selects")));
        assertTrue(qir.edges().stream().anyMatch(edge -> edge.relationship().equals("contains")));
    }

    @Test
    void rejectsPathTraversalOutsideWorkspace() {
        assertThrows(IllegalArgumentException.class,
                () -> scanner().scan(new ScanRequest("../outside", "local")));
    }

    private DefaultRepositoryScanner scanner() {
        return new DefaultRepositoryScanner(new KubernetesParser(), new DockerfileParser(), workspace.toString());
    }
}
