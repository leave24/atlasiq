package com.atlasiq.persistence;
import com.atlasiq.qir.QirModel;
import java.time.Instant;
public record StoredAnalysis(String id, Instant createdAt, QirModel model) {}
