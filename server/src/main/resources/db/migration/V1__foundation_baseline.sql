CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS database_baseline (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    baseline_key varchar(100) NOT NULL,
    description varchar(500),
    created_at timestamptz NOT NULL DEFAULT timezone('utc', now()),
    updated_at timestamptz NOT NULL DEFAULT timezone('utc', now()),
    created_by uuid,
    updated_by uuid,
    deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamptz,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT uk_database_baseline_key UNIQUE (baseline_key),
    CONSTRAINT ck_database_baseline_soft_delete CHECK (
        (deleted = false AND deleted_at IS NULL)
        OR
        (deleted = true AND deleted_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS ix_database_baseline_created_at
    ON database_baseline (created_at);
