package com.atlasiq.scanner;

import com.atlasiq.parser.docker.DockerfileParser;
import com.atlasiq.parser.dependencies.DependencyHintParser;
import com.atlasiq.parser.githubactions.GitHubActionsParser;
import com.atlasiq.parser.kubernetes.KubernetesParser;
import com.atlasiq.parser.terraform.TerraformParser;
import com.atlasiq.parser.api.ApiDiscoveryParser;
import com.atlasiq.parser.api.HttpClientDiscoveryParser;
import com.atlasiq.parser.database.DatabaseIntelligenceParser;
import com.atlasiq.parser.supplychain.SupplyChainParser;
import com.atlasiq.qir.ApiConsumerCorrelator;
import com.atlasiq.qir.Finding;
import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirModel;
import com.atlasiq.qir.QirNode;
import com.atlasiq.qir.QirScope;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class DefaultRepositoryScanner implements RepositoryScanner {

    private final KubernetesParser kubernetesParser;
    private final DockerfileParser dockerfileParser;
    private final GitHubActionsParser githubActionsParser;
    private final TerraformParser terraformParser;
    private final DependencyHintParser dependencyHintParser;
    private final ApiDiscoveryParser apiDiscoveryParser;
    private final HttpClientDiscoveryParser httpClientDiscoveryParser;
    private final ApiConsumerCorrelator apiConsumerCorrelator;
    private final DatabaseIntelligenceParser databaseIntelligenceParser;
    private final SupplyChainParser supplyChainParser;
    private final GitHubRepositoryAcquirer repositoryAcquirer;
    private final Path workspaceRoot;

    public DefaultRepositoryScanner(
            KubernetesParser kubernetesParser,
            DockerfileParser dockerfileParser,
            GitHubActionsParser githubActionsParser,
            TerraformParser terraformParser,
            DependencyHintParser dependencyHintParser,
            ApiDiscoveryParser apiDiscoveryParser,
            HttpClientDiscoveryParser httpClientDiscoveryParser,
            ApiConsumerCorrelator apiConsumerCorrelator,
            DatabaseIntelligenceParser databaseIntelligenceParser,
            SupplyChainParser supplyChainParser,
            GitHubRepositoryAcquirer repositoryAcquirer,
            @ConfigProperty(name = "atlasiq.workspace.root", defaultValue = "/workspace") String workspaceRoot) {
        this.kubernetesParser = kubernetesParser;
        this.dockerfileParser = dockerfileParser;
        this.githubActionsParser = githubActionsParser;
        this.terraformParser = terraformParser;
        this.dependencyHintParser = dependencyHintParser;
        this.apiDiscoveryParser = apiDiscoveryParser;
        this.httpClientDiscoveryParser = httpClientDiscoveryParser;
        this.apiConsumerCorrelator = apiConsumerCorrelator;
        this.databaseIntelligenceParser = databaseIntelligenceParser;
        this.supplyChainParser = supplyChainParser;
        this.repositoryAcquirer = repositoryAcquirer;
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
    }

    @Override
    public QirModel scan(ScanRequest request) {
        if (request == null || request.repository() == null || request.repository().isBlank()) {
            throw new IllegalArgumentException("repository is required");
        }

        if (request.repository().trim().startsWith("https://")) {
            var acquired = repositoryAcquirer.acquire(request.repository(), request.ref(), request.githubToken());
            try {
                return scanPath(request.repository(), acquired.ref(), acquired.path(), request.githubToken() == null || request.githubToken().isBlank() ? "public-github" : "github-app", request.system());
            } finally {
                repositoryAcquirer.cleanup(acquired.path());
            }
        }

        Path repositoryPath = resolveLocalRepository(request.repository());
        String ref = request.ref() == null || request.ref().isBlank() ? "local" : request.ref();
        return scanPath(request.repository(), ref, repositoryPath, "local-workspace", request.system());
    }

    private QirModel scanPath(String repository, String ref, Path repositoryPath, String source, String requestedSystem) {
        var kubernetes = kubernetesParser.parse(repositoryPath);
        var docker = dockerfileParser.parse(repositoryPath);
        var githubActions = githubActionsParser.parse(repositoryPath);
        var terraform = terraformParser.parse(repositoryPath);
        var dependencyHints = dependencyHintParser.parse(repositoryPath);
        var apiEndpoints = apiDiscoveryParser.parse(repositoryPath);
        var httpClients = httpClientDiscoveryParser.parse(repositoryPath);
        var databases = databaseIntelligenceParser.parse(repositoryPath);
        var packages = supplyChainParser.parse(repositoryPath);

        var nodes = new ArrayList<QirNode>();
        var edges = new ArrayList<QirEdge>();
        var findings = new ArrayList<Finding>();

        String repositoryId = repositoryId(repository, repositoryPath, source);
        String system = requestedSystem == null || requestedSystem.isBlank() ? "default" : requestedSystem.trim();

        nodes.add(new QirNode(
                repositoryId,
                "repository",
                repository,
                source,
                Map.of("ref", ref, "workspace", repositoryPath.toString(), "system", system)));
        nodes.addAll(kubernetes.nodes());
        nodes.addAll(docker.nodes());
        nodes.addAll(githubActions.nodes());
        nodes.addAll(terraform.nodes());
        nodes.addAll(dependencyHints);
        nodes.addAll(apiEndpoints);
        nodes.addAll(httpClients);
        nodes.addAll(databases.nodes());
        nodes.addAll(packages);
        edges.addAll(kubernetes.edges());
        edges.addAll(docker.edges());
        edges.addAll(githubActions.edges());
        edges.addAll(terraform.edges());
        edges.addAll(databases.edges());
        findings.addAll(kubernetes.findings());
        findings.addAll(docker.findings());
        findings.addAll(githubActions.findings());
        findings.addAll(terraform.findings());

        edges.addAll(apiConsumerCorrelator.correlate(nodes));

        nodes.stream()
                .filter(node -> !"repository".equals(node.type()))
                .forEach(node -> edges.add(new QirEdge(
                        repositoryId,
                        node.id(),
                        "contains",
                        Map.of("source", source))));

        return new QirModel(repository, ref, nodes, edges, findings, new QirScope(system, repositoryId));
    }

    private String repositoryId(String repository, Path repositoryPath, String source) {
        if ("local-workspace".equals(source)) {
            String canonicalPath = repositoryPath.toAbsolutePath().normalize().toString();
            String encoded = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(canonicalPath.getBytes(StandardCharsets.UTF_8));
            return "repo:local:" + encoded;
        }

        String normalized = repository.trim()
                .replaceFirst("(?i)^https://github\\.com/", "")
                .replaceFirst("\\.git$", "")
                .replaceAll("^/+|/+$", "");
        return "repo:github:" + normalized.toLowerCase();
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
