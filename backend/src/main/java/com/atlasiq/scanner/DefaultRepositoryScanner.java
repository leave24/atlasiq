package com.atlasiq.scanner;

import com.atlasiq.qir.QirModel;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DefaultRepositoryScanner implements RepositoryScanner {

    @Override
    public QirModel scan(ScanRequest request) {
        var nodes = new ArrayList<QirModel.QirNode>();
        var edges = new ArrayList<QirModel.QirEdge>();
        var findings = new ArrayList<QirModel.Finding>();

        nodes.add(new QirModel.QirNode(
                "repository",
                "repository",
                request.repository(),
                "github",
                Map.of("ref", request.ref())));

        return new QirModel(request.repository(), request.ref(), nodes, edges, findings);
    }
}
