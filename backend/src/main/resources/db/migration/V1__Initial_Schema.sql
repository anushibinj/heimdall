CREATE TABLE target_databases (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    engine VARCHAR(255) NOT NULL,
    host VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    db_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    cron_schedule VARCHAR(255) NOT NULL
);

CREATE TABLE snapshots (
    id UUID PRIMARY KEY,
    target_database_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT,
    checksum VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    CONSTRAINT fk_snapshot_target_database FOREIGN KEY (target_database_id) REFERENCES target_databases(id)
);
