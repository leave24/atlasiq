package com.atlasiq.scanner;

import com.atlasiq.parser.kubernetes.KubernetesParser;
import com.atlasiq.qir.Finding;
import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirModel;
import com.atlasiq.qir.QirNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

@ApplicationScoped
public class DefaultRepositoryScanner implements RepositoryScanner {

    private final KubernetesParser kubernetesParser;
    private final Path workspaceRoot;

    public DefaultRepositoryScanner(
            KubernetesParser kubernetesParser,
            @ConfigProperty(name = "atlasiq.workspace.root", defaultValue = "/workspace") String workspaceRoot) {
        this.kubernetesParser = kubernetesParser;
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
    }

    @Override
    public QirModel scan(ScanRequest request) {
        Path repositoryPath = resolveRepository(request.repository());
        var kubernetes = kubernetesParser.parse(repositoryPath);

        var nodes = new ArrayList<QirNode>();
        var edges = new ArrayList<QirEdge>();
        var findings = new ArrayList<Finding>();

        nodes.add(new QirNode(
                "repository",
                "repository",
                request.repository(),
                "local-workspace",
                Map.of("ref", request.ref(), "workspace", repositoryPath.toString())));
        nodes.addAll(kubernetes.nodes());
        edges.addAll(kubernetes.edges());
        findings.addAll(kubernetes.findings());

        kubernetes.nodes().forEach(node -> edges.add(new QirEdge(
                "repository",
                node.id(),
                "contains",
                Map.of("source", "workspace"))));

        return new QirModel(request.repository(), request.ref(), nodes, edges, findings);
    }

    private Path resolveRepository(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("repository is required");
        }

        Path candidate = workspaceRoot.resolve(repository).normalize();
        if (!candidate.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("repository must stay inside the configured workspace root");
        }
        if (!Files.isDirectory(candidate)) {
            throw new IllegalArgumentException("repository workspace does not exist: " + repository);
        }
        return candidate;
    }
}
