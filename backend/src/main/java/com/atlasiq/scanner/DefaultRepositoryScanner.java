package com.atlasiq.scanner;

import com.atlasiq.parser.docker.DockerfileParser;
import com.atlasiq.parser.githubactions.GitHubActionsParser;
import com.atlasiq.parser.kubernetes.KubernetesParser;
import com.atlasiq.parser.terraform.TerraformParser;
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
    private final DockerfileParser dockerfileParser;
    private final GitHubActionsParser githubActionsParser;
    private final TerraformParser terraformParser;
    private final GitHubRepositoryAcquirer repositoryAcquirer;
    private final Path workspaceRoot;

    public DefaultRepositoryScanner(
            KubernetesParser kubernetesParser,
            DockerfileParser dockerfileParser,
            GitHubActionsParser githubActionsParser,
            TerraformParser terraformParser,
            GitHubRepositoryAcquirer repositoryAcquirer,
            @ConfigProperty(name = "atlasiq.workspace.root", defaultValue = "/workspace") String workspaceRoot) {
        this.kubernetesParser = kubernetesParser;
        this.dockerfileParser = dockerfileParser;
        this.githubActionsParser = githubActionsParser;
        this.terraformParser = terraformParser;
        this.repositoryAcquirer = repositoryAcquirer;
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
    }

    @Override
    public QirModel scan(ScanRequest request) {
        if (request == null || request.repository() == null || request.repository().isBlank()) {
            throw new IllegalArgumentException("repository is required");
        }

        if (request.repository().trim().startsWith("https://")) {
            var acquired = repositoryAcquirer.acquire(request.repository(), request.ref());
            try {
                return scanPath(request.repository(), acquired.ref(), acquired.path(), "public-github");
            } finally {
                repositoryAcquirer.cleanup(acquired.path());
            }
        }

        Path repositoryPath = resolveLocalRepository(request.repository());
        String ref = request.ref() == null || request.ref().isBlank() ? "local" : request.ref();
        return scanPath(request.repository(), ref, repositoryPath, "local-workspace");
    }

    private QirModel scanPath(String repository, String ref, Path repositoryPath, String source) {
        var kubernetes = kubernetesParser.parse(repositoryPath);
        var docker = dockerfileParser.parse(repositoryPath);
        var githubActions = githubActionsParser.parse(repositoryPath);
        var terraform = terraformParser.parse(repositoryPath);

        var nodes = new ArrayList<QirNode>();
        var edges = new ArrayList<QirEdge>();
        var findings = new ArrayList<Finding>();

        nodes.add(new QirNode(
                "repository",
                "repository",
                repository,
                source,
                Map.of("ref", ref, "workspace", repositoryPath.toString())));
        nodes.addAll(kubernetes.nodes());
        nodes.addAll(docker.nodes());
        nodes.addAll(githubActions.nodes());
        nodes.addAll(terraform.nodes());
        edges.addAll(kubernetes.edges());
        edges.addAll(docker.edges());
        edges.addAll(githubActions.edges());
        edges.addAll(terraform.edges());
        findings.addAll(kubernetes.findings());
        findings.addAll(docker.findings());
        findings.addAll(githubActions.findings());
        findings.addAll(terraform.findings());

        nodes.stream()
                .filter(node -> !"repository".equals(node.type()))
                .forEach(node -> edges.add(new QirEdge(
                        "repository",
                        node.id(),
                        "contains",
                        Map.of("source", source))));

        return new QirModel(repository, ref, nodes, edges, findings);
    }

    private Path resolveLocalRepository(String repository) {
        Path candidate = workspaceRoot.resolve(repository).normalize();
        if (!candidate.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("repository must stay inside the configured workspace root");
        }
        if (!Files.isDirectory(candidate)) {
            throw new IllegalArgumentException(
                    "repository workspace does not exist: " + repository
                            + ". Use a public https://github.com/owner/repository URL or prepare a local workspace.");
        }
        return candidate;
    }
}
