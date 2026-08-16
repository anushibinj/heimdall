ALTER TABLE target_databases ADD COLUMN created_by_user_id UUID;
ALTER TABLE target_databases ADD CONSTRAINT fk_target_database_created_by
    FOREIGN KEY (created_by_user_id) REFERENCES users(id);
