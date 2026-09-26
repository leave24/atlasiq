package com.atlasiq.scanner;

import java.util.List;

public record MultiScanRequest(
        String system,
        List<ScanRequest> repositories) {
}
