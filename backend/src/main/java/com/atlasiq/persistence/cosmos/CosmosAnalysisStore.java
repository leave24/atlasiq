package com.atlasiq.persistence.cosmos;
import com.atlasiq.persistence.*;
import com.atlasiq.qir.QirModel;
import java.util.*;

/**
 * Cosmos DB adapter boundary.
 * Cosmos is intentionally not treated as JDBC/relational storage.
 * A production implementation can bind this port to the Azure Cosmos SDK
 * while the scanner/API continue depending only on AnalysisStore.
 */
public abstract class CosmosAnalysisStore implements AnalysisStore {
 protected abstract StoredAnalysis writeDocument(QirModel model);
 protected abstract Optional<StoredAnalysis> readDocument(String id);
 protected abstract List<StoredAnalysis> queryRecentDocuments(int limit);
 @Override public StoredAnalysis save(QirModel model){return writeDocument(model);}
 @Override public Optional<StoredAnalysis> find(String id){return readDocument(id);}
 @Override public List<StoredAnalysis> recent(int limit){return queryRecentDocuments(Math.max(1,Math.min(limit,100)));}
}
