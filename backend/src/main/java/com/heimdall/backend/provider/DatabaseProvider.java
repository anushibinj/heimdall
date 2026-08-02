package com.heimdall.backend.provider;

import com.heimdall.backend.entity.TargetDatabase;

public interface DatabaseProvider {

    /**
     * Checks if this provider supports the given database engine.
     * @param engine e.g., "POSTGRES"
     * @return true if supported
     */
    boolean supports(String engine);

    /**
     * Tests the connection to the target database.
     * @param database The target database configuration
     * @return true if successful
     * @throws Exception if connection fails
     */
    boolean testConnection(TargetDatabase database) throws Exception;

    /**
     * Executes the backup for the target database.
     * @param database The target database configuration
     * @param backupFilePath The destination path for the dump file
     * @throws Exception if backup fails
     */
    void executeBackup(TargetDatabase database, String backupFilePath) throws Exception;
}
