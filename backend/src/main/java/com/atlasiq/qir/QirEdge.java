package com.atlasiq.qir;

import java.util.Map;

public record QirEdge(
        String from,
        String to,
        String relationship,
        Map<String, Object> metadata) {}
