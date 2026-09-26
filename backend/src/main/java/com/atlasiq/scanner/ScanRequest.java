package com.atlasiq.scanner;

public record ScanRequest(String repository, String ref, String system, String githubToken) {

    public ScanRequest(String repository, String ref) {
        this(repository, ref, null, null);
    }

    public ScanRequest(String repository, String ref, String system) {
        this(repository, ref, system, null);
    }
}
