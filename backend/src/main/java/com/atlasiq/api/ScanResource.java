package com.atlasiq.api;

import com.atlasiq.qir.QirModel;
import com.atlasiq.scanner.RepositoryScanner;
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

    public ScanResource(RepositoryScanner scanner) {
        this.scanner = scanner;
    }

    @POST
    public QirModel scan(ScanRequest request) {
        return scanner.scan(new com.atlasiq.scanner.ScanRequest(request.repository(), request.ref()));
    }
}
