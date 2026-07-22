# Error Log

Only real observed errors or warnings are recorded here. Do not add simulated errors. Do not include tokens, passwords, station secrets, webhook signatures, or production credentials.

## ERR-001

Date: 2026-07-21

Category: Deployment

Module: Local runtime

Environment: Local development

Description: Backend live run against default `jdbc:postgresql://localhost:5432/nexus` failed because no local PostgreSQL service was available.

Expected result: Backend starts with a reachable PostgreSQL database.

Actual result: JDBC connection errors occurred during runtime/background jobs.

Root cause: Local PostgreSQL was not running on `localhost:5432`.

Resolution: Use Docker Compose PostgreSQL or start/configure local PostgreSQL with matching `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

Status: Open environmental issue.

## ERR-002

Date: 2026-07-21

Category: Build

Module: Frontend

Environment: Local build

Description: Vite production build reported a main JavaScript chunk larger than 500 KB.

Expected result: Production chunks remain within target warning thresholds.

Actual result: Build passed but emitted a chunk-size warning.

Root cause: Current frontend routes/features are bundled into a large main chunk.

Resolution: Add route-level dynamic imports/code splitting before production hardening if bundle growth continues.

Status: Open warning.

## ERR-003

Date: 2026-07-21

Category: Build

Module: Backend

Environment: Local build

Description: Gradle build reported deprecated API usage in `GlobalExceptionHandler`.

Expected result: Build has no deprecation warnings.

Actual result: Build passed with a deprecation warning.

Root cause: At least one deprecated API is referenced.

Resolution: Recompile with `-Xlint:deprecation`, identify API, and update in a maintenance task.

Status: Open warning.

## ERR-004

Date: 2026-07-21

Category: Deployment

Module: Docker Compose validation

Environment: Local development

Description: `docker --version` failed because Docker CLI is not installed or not available on PATH.

Expected result: Docker CLI reports its installed version and allows Docker Compose smoke validation.

Actual result: PowerShell returned `docker` is not recognized as a cmdlet, function, script file, or operable program.

Root cause: Docker Desktop or compatible Docker Engine/Compose plugin is unavailable in the current environment.

Resolution: Install Docker Desktop or expose Docker CLI on PATH, then run `docker compose up --build` and health checks from [Deployment Guide](Deployment-Guide.md).

Status: Open environmental issue.
