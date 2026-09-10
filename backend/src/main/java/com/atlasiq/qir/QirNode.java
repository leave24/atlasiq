package com.atlasiq.qir;

import java.util.Map;

public record QirNode(
        String id,
        String type,
        String name,
        String technology,
        Map<String, Object> metadata) {}
