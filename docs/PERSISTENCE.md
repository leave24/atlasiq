# AtlasIQ persistence

AtlasIQ persistence is isolated behind the `AnalysisStore` port. Scanner, QIR and API code do not depend on a database vendor.

## Relational engines

The JDBC adapter supports the same AtlasIQ schema and JSON snapshot model on:

| Engine | ATLASIQ_DB_KIND | JDBC example |
| --- | --- | --- |
| PostgreSQL | postgresql | jdbc:postgresql://db:5432/atlasiq |
| MySQL | mysql | jdbc:mysql://db:3306/atlasiq |
| SQL Server | mssql | jdbc:sqlserver://db:1433;databaseName=atlasiq;encrypt=true |

Set `ATLASIQ_DATABASE_USER` and `ATLASIQ_DATABASE_PASSWORD` from a secret provider. Never commit production credentials.

The initial model stores immutable QIR analysis snapshots. This makes architecture history, diffs and the future Time Machine possible without coupling QIR objects to ORM entities.

## Cosmos DB

Cosmos DB is not forced through JDBC. `CosmosAnalysisStore` defines a document-store adapter boundary implementing the same `AnalysisStore` contract. A production Cosmos deployment should bind an Azure Cosmos SDK implementation and use a partition strategy such as organization/system plus analysis id.

This separation allows AtlasIQ deployments to choose relational or document persistence without changing the analysis engine.

## Production notes

For production use, schema evolution should move from bootstrap DDL to versioned migrations (Flyway/Liquibase), credentials should come from Kubernetes/secret management, TLS should be required for external databases, and database backups/retention should follow the deployment's recovery objectives.
