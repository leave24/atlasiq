package com.atlasiq.scanner;

import com.atlasiq.qir.QirModel;

public interface RepositoryScanner {
    QirModel scan(ScanRequest request);
}
