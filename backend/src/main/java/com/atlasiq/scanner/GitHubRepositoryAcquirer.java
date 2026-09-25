package com.atlasiq.scanner;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;

@ApplicationScoped
public class GitHubRepositoryAcquirer {

    private final Path workspaceRoot;

    public GitHubRepositoryAcquirer(
            @ConfigProperty(name = "atlasiq.workspace.root", defaultValue = "/workspace") String workspaceRoot) {
        this.workspaceRoot = Path.of(workspaceRoot).toAbsolutePath().normalize();
    }

    public AcquiredRepository acquire(String repository, String ref) {
        URI uri = validatePublicGitHubUrl(repository);
        String normalizedRef = normalizeRef(ref);
        String repoName = repositoryName(uri);
        Path target = workspaceRoot.resolve(".atlasiq-" + repoName + "-" + UUID.randomUUID()).normalize();

        if (!target.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("repository target must stay inside the configured workspace root");
        }

        try {
            Files.createDirectories(workspaceRoot);
            var command = new java.util.ArrayList<String>();
            command.add("git");
            command.add("clone");
            command.add("--depth=1");
            command.add("--no-tags");
            if (normalizedRef != null) {
                command.add("--branch=" + normalizedRef);
            }
            command.add(uri.toString());
            command.add(target.toString());

            ProcessBuilder processBuilder = new ProcessBuilder(command)
                    .redirectErrorStream(true);
            processBuilder.environment().put("GIT_TERMINAL_PROMPT", "0");
            processBuilder.environment().put("GCM_INTERACTIVE", "Never");
            processBuilder.environment().put("GIT_ASKPASS", "echo");
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                deleteRecursively(target);
                throw new IllegalArgumentException("unable to clone public GitHub repository: " + sanitize(output));
            }
            return new AcquiredRepository(target, normalizedRef == null ? "default" : normalizedRef);
        } catch (IOException e) {
            deleteRecursively(target);
            throw new IllegalStateException("unable to prepare repository workspace", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            deleteRecursively(target);
            throw new IllegalStateException("repository acquisition interrupted", e);
        }
    }

    public void cleanup(Path path) {
        if (path != null && path.normalize().startsWith(workspaceRoot)) {
            deleteRecursively(path);
        }
    }

    static URI validatePublicGitHubUrl(String repository) {
        if (repository == null || repository.isBlank()) {
            throw new IllegalArgumentException("repository is required");
        }
        final URI uri;
        try {
            uri = URI.create(repository.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("repository must be a valid GitHub HTTPS URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"github.com".equalsIgnoreCase(uri.getHost())
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || uri.getQuery() != null
                || uri.getFragment() != null) {
            throw new IllegalArgumentException("only public https://github.com/owner/repository URLs are supported");
        }
        String[] segments = uri.getPath().split("/");
        if (segments.length != 3 || segments[1].isBlank() || segments[2].isBlank()) {
            throw new IllegalArgumentException("repository URL must match https://github.com/owner/repository");
        }
        return uri;
    }

    static String normalizeRef(String ref) {
        if (ref == null || ref.isBlank() || "default".equalsIgnoreCase(ref)) {
            return null;
        }
        String value = ref.trim();
        if (value.startsWith("-") || value.contains("..") || !value.matches("[A-Za-z0-9._/-]+")) {
            throw new IllegalArgumentException("ref contains unsupported characters");
        }
        return value;
    }

    private static String repositoryName(URI uri) {
        String name = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1);
        return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }

    private static String sanitize(String output) {
        if (output == null || output.isBlank()) return "git clone failed";
        String oneLine = output.replaceAll("[\\r\\n]+", " ").trim();
        return oneLine.length() > 300 ? oneLine.substring(0, 300) : oneLine;
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    public record AcquiredRepository(Path path, String ref) {}
}
