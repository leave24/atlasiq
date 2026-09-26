CREATE TABLE IF NOT EXISTS atlasiq_analysis (
 id VARCHAR(36) PRIMARY KEY,
 created_at TIMESTAMP NOT NULL,
 repository VARCHAR(1024) NOT NULL,
 ref_name VARCHAR(512),
 system_name VARCHAR(512),
 organization_id VARCHAR(128),
 workspace_id VARCHAR(128),
 qir_version VARCHAR(32) NOT NULL DEFAULT '1.0',
 qir_json TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_atlasiq_analysis_created ON atlasiq_analysis(created_at);
CREATE INDEX IF NOT EXISTS idx_atlasiq_analysis_repo_system_created ON atlasiq_analysis(repository,system_name,created_at);
CREATE INDEX IF NOT EXISTS idx_atlasiq_analysis_tenant_created ON atlasiq_analysis(organization_id,workspace_id,created_at);
