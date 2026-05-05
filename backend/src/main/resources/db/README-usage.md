# DB Script Guide

## Current entry points

- `schema_v2_prd.sql`: current PRD v2 schema
- `temp_data.sql`: current mock data for local use
- `init_all.sql`: schema + temp data bootstrap
- `init-db.cmd`: Windows wrapper for `init_all.sql`

## Compatibility file

- `min_demo_data_v2.sql`: compatibility alias that points to `temp_data.sql`

## Legacy files

- `archive/legacy-sql/`: old schema, migration, rollback, and patch scripts kept only for reference

## Usage

1. For a fresh local database, run `init_all.sql`.
2. On Windows, you can run `init-db.cmd`.
3. If the schema already exists, run `temp_data.sql` to refresh mock data only.
4. Cuiyuan community enrichment is now built into `temp_data.sql`.
