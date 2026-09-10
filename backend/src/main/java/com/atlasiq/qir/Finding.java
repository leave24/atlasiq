package com.atlasiq.qir;

public record Finding(String id, String severity, String category, String resourceId, String title,
                      String recommendation) {
}
