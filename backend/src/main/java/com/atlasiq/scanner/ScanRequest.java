package com.atlasiq.scanner;

public record ScanRequest(String repository, String ref, String system) {

    public ScanRequest(String repository, String ref) {
        this(repository, ref, null);
    }
}
