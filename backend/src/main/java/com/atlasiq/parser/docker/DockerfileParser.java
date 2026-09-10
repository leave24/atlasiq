package com.atlasiq.parser.docker;

import com.atlasiq.qir.Finding;
import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApplicationScoped
public class DockerfileParser {

    private static final Pattern SENSITIVE_ENV = Pattern.compile("(?i).*(password|passwd|secret|token|api[_-]?key|private[_-]?key).*", Pattern.CASE_INSENSITIVE);

    public DockerAnalysis parse(Path root) {
        var nodes = new ArrayList<QirNode>();
        var findings = new ArrayList<Finding>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isDockerfile)
                    .forEach(path -> parseFile(root, path, nodes, findings));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to scan Dockerfiles under " + root, e);
        }

        return new DockerAnalysis(List.copyOf(nodes), List.of(), List.copyOf(findings));
    }

    private void parseFile(Path root, Path path, List<QirNode> nodes, List<Finding> findings) {
        try {
            List<String> lines = Files.readAllLines(path);
            String source = root.relativize(path).toString().replace('\\', '/');
            String id = "docker:" + source;
            List<String> baseImages = new ArrayList<>();
            boolean hasUser = false;

            for (String raw : lines) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String upper = line.toUpperCase(Locale.ROOT);

                if (upper.startsWith("FROM ")) {
                    String image = line.substring(5).trim().split("\\s+")[0];
                    baseImages.add(image);
                    if (isUnpinned(image)) {
                        findings.add(finding(id, source, "MEDIUM", "supply-chain",
                                "Base image is not pinned: " + image,
                                "Pin the base image to an immutable digest or an explicit version tag."));
                    }
                } else if (upper.startsWith("USER ")) {
                    hasUser = true;
                    String user = line.substring(5).trim();
                    if ("root".equalsIgnoreCase(user) || "0".equals(user)) {
                        findings.add(finding(id, source, "HIGH", "security",
                                "Dockerfile explicitly runs as root",
                                "Use a dedicated non-root USER for the runtime stage."));
                    }
                } else if (upper.startsWith("ENV ")) {
                    inspectEnv(id, source, line.substring(4).trim(), findings);
                }
            }

            if (!hasUser) {
                findings.add(finding(id, source, "HIGH", "security",
                        "Dockerfile does not declare USER",
                        "Declare a dedicated non-root USER in the final runtime stage."));
            }

            nodes.add(new QirNode(id, "container-image", path.getFileName().toString(), "docker",
                    Map.of("source", source, "baseImages", List.copyOf(baseImages))));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to parse Dockerfile " + path, e);
        }
    }

    private void inspectEnv(String id, String source, String definition, List<Finding> findings) {
        String key = definition.split("[=\\s]", 2)[0];
        if (SENSITIVE_ENV.matcher(key).matches() && definition.contains("=")) {
            String value = definition.substring(definition.indexOf('=') + 1).trim();
            if (!value.isEmpty() && !value.startsWith("${") && !value.startsWith("$")) {
                findings.add(finding(id, source, "HIGH", "secrets",
                        "Possible secret embedded in ENV: " + key,
                        "Inject secrets at runtime instead of baking them into the image."));
            }
        }
    }

    private boolean isDockerfile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.equals("dockerfile") || name.startsWith("dockerfile.");
    }

    private boolean isUnpinned(String image) {
        if (image.contains("@sha256:")) return false;
        int lastSlash = image.lastIndexOf('/');
        int colon = image.lastIndexOf(':');
        return colon <= lastSlash || image.endsWith(":latest");
    }

    private Finding finding(String resourceId, String source, String severity, String category, String title, String recommendation) {
        return new Finding(resourceId + ":" + Integer.toHexString(title.hashCode()), severity, category, resourceId,
                title + " [" + source + "]", recommendation);
    }
}
