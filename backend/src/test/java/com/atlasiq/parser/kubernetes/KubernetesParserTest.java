package com.atlasiq.parser.kubernetes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KubernetesParserTest {

    @TempDir
    Path tempDir;

    @Test
    void reconstructsIngressServiceDeploymentAndConfiguration() throws Exception {
        Files.writeString(tempDir.resolve("stack.yaml"), """
                apiVersion: v1
                kind: ConfigMap
                metadata:
                  name: app-config
                  namespace: atlasiq
                ---
                apiVersion: v1
                kind: ServiceAccount
                metadata:
                  name: api
                  namespace: atlasiq
                ---
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: api
                  namespace: atlasiq
                spec:
                  selector:
                    matchLabels:
                      app: api
                  template:
                    metadata:
                      labels:
                        app: api
                    spec:
                      serviceAccountName: api
                      securityContext:
                        runAsNonRoot: true
                      containers:
                        - name: api
                          image: atlasiq/backend:local
                          envFrom:
                            - configMapRef:
                                name: app-config
                          resources:
                            requests: { cpu: 100m, memory: 128Mi }
                            limits: { cpu: 500m, memory: 512Mi }
                          readinessProbe:
                            httpGet: { path: /q/health/ready, port: 8080 }
                          livenessProbe:
                            httpGet: { path: /q/health/live, port: 8080 }
                ---
                apiVersion: v1
                kind: Service
                metadata:
                  name: api
                  namespace: atlasiq
                spec:
                  selector:
                    app: api
                  ports:
                    - port: 8080
                ---
                apiVersion: networking.k8s.io/v1
                kind: Ingress
                metadata:
                  name: atlasiq
                  namespace: atlasiq
                spec:
                  rules:
                    - host: atlasiq.local
                      http:
                        paths:
                          - path: /api
                            pathType: Prefix
                            backend:
                              service:
                                name: api
                                port:
                                  number: 8080
                """);

        KubernetesAnalysis result = new KubernetesParser().parse(tempDir);

        assertTrue(result.nodes().stream().anyMatch(n -> n.id().equals("k8s:atlasiq:ingress:atlasiq")));
        assertTrue(result.edges().stream().anyMatch(e -> e.relationship().equals("routes_to")));
        assertTrue(result.edges().stream().anyMatch(e -> e.relationship().equals("selects")));
        assertTrue(result.edges().stream().anyMatch(e -> e.relationship().equals("reads_config")));
        assertTrue(result.edges().stream().anyMatch(e -> e.relationship().equals("uses_service_account")));
        assertTrue(result.findings().isEmpty(), () -> "Unexpected findings: " + result.findings());
    }

    @Test
    void reportsUnsafeWorkloadDefaults() throws Exception {
        Files.writeString(tempDir.resolve("unsafe.yaml"), """
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: unsafe
                  namespace: atlasiq
                spec:
                  template:
                    metadata:
                      labels: { app: unsafe }
                    spec:
                      hostNetwork: true
                      volumes:
                        - name: host
                          hostPath: { path: /tmp }
                      containers:
                        - name: unsafe
                          image: example:latest
                          securityContext:
                            privileged: true
                """);

        KubernetesAnalysis result = new KubernetesParser().parse(tempDir);

        assertTrue(result.findings().stream().anyMatch(f -> f.severity().equals("CRITICAL")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("hostNetwork")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("hostPath")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("readinessProbe")));
        assertTrue(result.findings().stream().anyMatch(f -> f.title().contains("resource requests/limits")));
    }
}
