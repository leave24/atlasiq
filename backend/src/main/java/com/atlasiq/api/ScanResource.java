package com.atlasiq.api;

import com.atlasiq.qir.QirAggregator;
import com.atlasiq.qir.QirModel;
import com.atlasiq.scanner.MultiScanRequest;
import com.atlasiq.scanner.RepositoryScanner;
import com.atlasiq.scanner.ScanRequest;
import com.atlasiq.persistence.AnalysisStore;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/scans")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ScanResource {

    private final RepositoryScanner scanner;
    private final QirAggregator aggregator;
    private final AnalysisStore store;

    public ScanResource(RepositoryScanner scanner, QirAggregator aggregator, AnalysisStore store) {
        this.scanner = scanner;
        this.aggregator = aggregator;
        this.store = store;
    }

    @POST
    public QirModel scan(ScanRequest request) {
        var model = scanner.scan(request);
        store.save(model);
        return model;
    }

    @POST
    @Path("/multi")
    public QirModel scanMultiple(MultiScanRequest request) {
        if (request == null || request.system() == null || request.system().isBlank()) {
            throw new IllegalArgumentException("system is required");
        }
        if (request.repositories() == null || request.repositories().isEmpty()) {
            throw new IllegalArgumentException("at least one repository is required");
        }
        var models = request.repositories().stream()
                .map(repo -> scanner.scan(new ScanRequest(repo.repository(), repo.ref(), request.system())))
                .toList();
        var model = aggregator.aggregate(request.system(), models);
        store.save(model);
        return model;
    }
}
