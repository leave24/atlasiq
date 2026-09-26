package com.atlasiq.persistence;
import com.atlasiq.qir.QirModel;
import java.util.*;
public interface AnalysisStore {
 StoredAnalysis save(QirModel model);
 Optional<StoredAnalysis> find(String id);
 List<StoredAnalysis> recent(int limit);
 List<StoredAnalysis> history(String repository, String system, int limit);
}
