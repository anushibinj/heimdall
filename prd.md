# Product Requirements Document: Project Heimdall

## 1. Project Overview

**Project Heimdall** is an automated database snapshot and restoration management tool. It provides a centralized dashboard to configure target databases, schedule daily automated backups, and perform frictionless, one-click force-restorations. The MVP focuses exclusively on PostgreSQL, with a foundational architecture designed for future extensibility to other database engines.

## 2. Technical Stack & Workspace Structure

The project will be housed in a single monorepo to streamline deployment and version control.

* **Workspace:**
* `/backend` - Spring Boot (Java)
* `/frontend` - React JS + TypeScript (bundled via Vite for rapid development loops)


* **Backend Stack:** Spring Boot, Spring Data JPA, Spring Boot Starter Quartz (for scheduling), H2 Database (File-persisted).
* **Frontend Stack:** React, TypeScript, Vite.
* **Storage:** Local filesystem for storing dump files.

## 3. Core Features & Requirements

### 3.1 Target Database Management

* **Add Database:** Users can register a new target database by providing the connection details (Host, Port, DB Name, Username, Password).
* **Test Connection:** The system must verify credentials and connectivity before saving.
* **Extensibility:** The configuration must include a generic `engine` type (defaulting to `POSTGRES`) to support future integrations.

### 3.2 Automated Snapshot Generation (Connection-Aware)

* **Scheduling:** Each configured database will have an automated daily job (configurable via Cron expression).
* **Safe Execution (Zero-Interruption):** Before executing `pg_dump`, Heimdall will poll the database's connection state. It will wait until there are **no active connections** (excluding its own polling connection) before initiating the snapshot. This ensures data consistency without abruptly terminating active client sessions.
* *Timeout/Retry Logic:* The polling mechanism will check the connection state at regular intervals. If active connections persist beyond a configurable timeout window, the job will fail gracefully and log a warning.


* **Checksum & Deduplication:**
* Before saving a new snapshot, the system generates a cryptographic hash (e.g., SHA-256) of the raw data.
* If the generated hash matches the hash of the most recent successful snapshot, the new snapshot is discarded to save filesystem space, and the system logs a "Skipped - No Changes" event.


* **Storage:** Dumps are saved directly to the local filesystem under a configurable root directory, grouped by database ID.

### 3.3 Snapshot Restoration

* **Zero-Conflict Restoration:** The system must forcefully apply snapshots without conflicts or lock issues.
* **Aggressive Connection Termination:** Prior to restoration, the backend must execute `SELECT pg_terminate_backend(pid)` to forcefully kick all active sessions off the target database.
* **Clean Overwrite:** The restore process will drop the existing schema and recreate it using `pg_restore --clean` (or the `pg_dump` equivalent flags).

### 3.4 User Interface

* **Dashboard:** A high-level overview of all configured databases, their health/connectivity status, and the timestamp of their last successful backup.
* **Snapshot Browser:** A detailed view for a specific database, listing all available historical snapshots, their file sizes, and checksum hashes.
* **Design Language:** A minimalist, high-contrast aesthetic utilizing deep navy backgrounds, pitch white text elements, and clean, sans-serif typography to ensure high legibility and a developer-focused feel.

---

## 4. System Architecture & Workflows

### 4.1 Checksum Strategy (The Hash Dilemma)

Standard `pg_dump` includes execution timestamps and metadata, meaning file hashes will differ even if table data has not changed.

* **Implementation:** Heimdall will run `pg_dump` with flags that exclude metadata (e.g., `--data-only` piped through a hash function, or querying a hash of `pg_stat_user_tables` rows) to calculate the deterministic state of the data.
* **Archive:** The actual backup saved to disk will be the full schema + data dump (`pg_dump -Fc`), but the deduplication logic will rely on the deterministic data hash.

### 4.2 Local Persistence (H2 Database Schema)

Heimdall's internal state will be persisted in a local H2 database configured for file storage (`jdbc:h2:file:./heimdall-data/config-db`).

**Table: `target_databases**`

| Column | Type | Description |
| --- | --- | --- |
| `id` | UUID (PK) | Unique identifier |
| `name` | VARCHAR | User-friendly alias |
| `engine` | VARCHAR | e.g., `POSTGRES` |
| `host` | VARCHAR | Database host IP/Domain |
| `port` | INT | Database port |
| `db_name` | VARCHAR | Target database name |
| `username` | VARCHAR | Auth username |
| `password` | VARCHAR | Encrypted password |
| `cron_schedule` | VARCHAR | Daily schedule expression |

**Table: `snapshots**`

| Column | Type | Description |
| --- | --- | --- |
| `id` | UUID (PK) | Unique identifier |
| `target_database_id` | UUID (FK) | Relation to configured DB |
| `created_at` | TIMESTAMP | Execution time |
| `file_path` | VARCHAR | Absolute path on filesystem |
| `file_size_bytes` | BIGINT | Size of the dump |
| `checksum` | VARCHAR | SHA-256 hash of the data state |
| `status` | VARCHAR | `SUCCESS`, `FAILED`, `SKIPPED`, `TIMEOUT` |

---

## 5. Development Phases

### Phase 1: Foundation & Backend Core

1. Initialize monorepo with `/backend` and `/frontend`.
2. Set up Spring Boot (using https://start.spring.io/) with H2 file persistence and define JPA entities.
3. Implement the Strategy Pattern (`DatabaseProvider` interface).
4. Implement `PostgresProvider` (Connection testing, OS-level `pg_dump` execution).

### Phase 2: Checksum, Polling & Scheduling Logic

1. Implement the Postgres connection polling logic using `pg_stat_activity` to ensure zero active client connections before backup execution.
2. Implement the deterministic hashing logic for Postgres data deduplication.
3. Integrate Spring Quartz to trigger daily jobs based on the H2 database configurations.
4. Implement the "Skip if checksum matches" validation flow.
5. Add Spring Flyway to version control the application's config schema.

### Phase 3: Restoration Engine

1. Implement aggressive connection termination queries (`pg_terminate_backend`) for Postgres.
2. Build the forced `pg_restore` execution logic.
3. Add rollback safety nets (temporary backups before overriding).

### Phase 4: Frontend & API Integration

1. Develop REST APIs in Spring Boot for CRUD operations on databases and snapshots.
2. Build the React dashboard, connection configuration forms, and snapshot data tables.
3. Wire up the "Revert to this" button to trigger the restoration pipeline.
4. Apply the minimalist deep navy/pitch white styling.

### Phase 5: Hardening

1. Implement symmetric encryption for database passwords stored in H2.
2. Add filesystem cleanup policies (e.g., delete snapshots older than X days to prevent disk exhaustion).
3. Extensive error handling for failed `ProcessBuilder` executions and backup timeouts.