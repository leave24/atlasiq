package com.atlasiq.scanner;

import com.atlasiq.qir.QirModel;

public interface RepositoryScanner {
    QirModel scan(ScanRequest request);

    record ScanRequest(String repository, String ref) {}
}
