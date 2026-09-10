package com.atlasiq.api;

import jakarta.validation.constraints.NotBlank;

public record ScanRequest(@NotBlank String repository, @NotBlank String ref) {
}
