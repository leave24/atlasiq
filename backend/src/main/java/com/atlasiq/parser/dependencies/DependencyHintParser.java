package com.atlasiq.parser.dependencies;

import com.atlasiq.qir.QirNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApplicationScoped
public class DependencyHintParser {

    private static final Pattern URL = Pattern.compile("https?://[A-Za-z0-9._-]+(?::\\d+)?(?:/[^\\s\"']*)?");
    private static final List<String> CONFIG_NAMES = List.of(
            "application.properties", "application.yml", "application.yaml",
            ".env", ".env.example", "docker-compose.yml", "docker-compose.yaml");

    public List<QirNode> parse(Path repository) {
        var nodes = new ArrayList<QirNode>();
        try (Stream<Path> files = Files.walk(repository, 8)) {
            files.filter(Files::isRegularFile)
                    .filter(this::supported)
                    .forEach(file -> parseFile(repository, file, nodes));
        } catch (IOException e) {
            throw new IllegalStateException("failed to inspect dependency configuration", e);
        }
        return List.copyOf(nodes);
    }

    private void parseFile(Path root, Path file, List<QirNode> nodes) {
        try {
            String content = Files.readString(file);
            var matcher = URL.matcher(content);
            int index = 0;
            while (matcher.find()) {
                String url = matcher.group();
                String relative = root.relativize(file).toString().replace('\\', '/');
                nodes.add(new QirNode(
                        "dependency:" + relative + ":" + index++,
                        "dependency-reference",
                        relative,
                        "configuration",
                        Map.of("targetUrl", url, "evidenceFile", relative, "evidenceKind", "declared-url")));
            }
        } catch (IOException ignored) {
            // A single unreadable config file must not abort repository analysis.
        }
    }

    private boolean supported(Path file) {
        String name = file.getFileName().toString();
        return CONFIG_NAMES.contains(name) || name.endsWith(".properties");
    }
}
