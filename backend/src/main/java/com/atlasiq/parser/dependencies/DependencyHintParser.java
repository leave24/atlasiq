package com.atlasiq.parser.dependencies;

import com.atlasiq.qir.QirNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApplicationScoped
public class DependencyHintParser {

    private static final Pattern URL = Pattern.compile("https?://(?![^/\\s\"'#]*@)(?:\\[[0-9A-Fa-f:.]+\\]|[A-Za-z0-9._-]+)(?::\\d+)?(?:/[^\\s\"']*)?(?:\\?[^\\s\"'#]*)?(?:#[^\\s\"']*)?");
    private static final Pattern HOST = Pattern.compile("[A-Za-z0-9_](?:[A-Za-z0-9_-]*[A-Za-z0-9_])?(?:\\.[A-Za-z0-9_](?:[A-Za-z0-9_-]*[A-Za-z0-9_])?)*");
    private static final Pattern IPV6 = Pattern.compile("\\[[0-9A-Fa-f:.]+\\]");
    private static final List<String> CONFIG_NAMES = List.of(
            "application.properties", "application.yml", "application.yaml",
            ".env", ".env.example", "docker-compose.yml", "docker-compose.yaml");
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git", ".idea", ".gradle", "target", "build", "dist", "node_modules", "vendor", ".next");

    public List<QirNode> parse(Path repository) {
        var nodes = new ArrayList<QirNode>();
        Path root = repository.toAbsolutePath().normalize();
        try (Stream<Path> files = Files.walk(root, 8)) {
            files.filter(path -> !excluded(root, path))
                    .filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> !Files.isSymbolicLink(path))
                    .filter(this::supported)
                    .forEach(file -> parseFile(root, file, nodes));
        } catch (IOException e) {
            throw new IllegalStateException("failed to inspect dependency configuration", e);
        }
        return List.copyOf(nodes);
    }

    private void parseFile(Path root, Path file, List<QirNode> nodes) {
        try {
            if (Files.isSymbolicLink(file)) return;
            Path real = file.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!real.startsWith(root.toRealPath())) return;

            String content = Files.readString(real);
            var matcher = URL.matcher(content);
            int index = 0;
            while (matcher.find()) {
                String sanitized = sanitize(matcher.group());
                if (sanitized == null) continue;
                String relative = root.relativize(file).toString().replace('\\', '/');
                nodes.add(new QirNode(
                        "dependency:" + relative + ":" + index++,
                        "dependency-reference",
                        relative,
                        "configuration",
                        Map.of("targetUrl", sanitized, "evidenceFile", relative, "evidenceKind", "declared-url")));
            }
        } catch (IOException | IllegalArgumentException ignored) {
            // A single unsafe or unreadable config file must not abort repository analysis.
        }
    }

    private String sanitize(String value) {
        int schemeEnd = value.indexOf("://");
        if (schemeEnd <= 0) return null;

        String scheme = value.substring(0, schemeEnd).toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) return null;

        int authorityStart = schemeEnd + 3;
        int authorityEnd = value.length();
        for (int i = authorityStart; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '/' || ch == '?' || ch == '#') {
                authorityEnd = i;
                break;
            }
        }

        String authority = value.substring(authorityStart, authorityEnd);
        if (authority.isBlank() || authority.contains("@")) return null;

        if (authority.startsWith("[")) {
            int bracketEnd = authority.indexOf(']');
            if (bracketEnd <= 0) return null;

            String host = authority.substring(0, bracketEnd + 1);
            String remainder = authority.substring(bracketEnd + 1);
            if (!IPV6.matcher(host).matches()) return null;
            if (remainder.isEmpty()) return scheme + "://" + host;
            if (!remainder.startsWith(":")) return null;

            String port = remainder.substring(1);
            if (port.isBlank() || !port.chars().allMatch(Character::isDigit)) return null;
            return scheme + "://" + host + ":" + port;
        }

        String host = authority;
        String port = "";
        int colon = authority.lastIndexOf(':');
        if (colon >= 0) {
            host = authority.substring(0, colon);
            port = authority.substring(colon + 1);
            if (port.isBlank() || !port.chars().allMatch(Character::isDigit)) return null;
            port = ":" + port;
        }
        if (!HOST.matcher(host).matches()) return null;

        return scheme + "://" + host + port;
    }

    private boolean excluded(Path root, Path path) {
        Path relative = root.relativize(path.toAbsolutePath().normalize());
        for (Path part : relative) {
            if (EXCLUDED_DIRECTORIES.contains(part.toString())) return true;
        }
        return false;
    }

    private boolean supported(Path file) {
        String name = file.getFileName().toString();
        return CONFIG_NAMES.contains(name) || name.endsWith(".properties");
    }
}
