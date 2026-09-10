package com.atlasiq.scanner;

import com.atlasiq.qir.Finding;
import com.atlasiq.qir.QirEdge;
import com.atlasiq.qir.QirModel;
import com.atlasiq.qir.QirNode;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Map;

@ApplicationScoped
public class DefaultRepositoryScanner implements RepositoryScanner {

    @Override
    public QirModel scan(ScanRequest request) {
        var nodes = new ArrayList<QirNode>();
        var edges = new ArrayList<QirEdge>();
        var findings = new ArrayList<Finding>();

        nodes.add(new QirNode(
                "repository",
                "repository",
                request.repository(),
                "github",
                Map.of("ref", request.ref())));

        return new QirModel(request.repository(), request.ref(), nodes, edges, findings);
    }
}
