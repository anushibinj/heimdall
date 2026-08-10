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
     * @return The log output of the backup process
     * @throws Exception if backup fails
     */
    String executeBackup(TargetDatabase database, String backupFilePath) throws Exception;

    /**
     * Polls the database until there are zero active connections or a timeout is reached.
     * @param database The target database configuration
     * @param timeoutMillis Maximum time to wait in milliseconds
     * @throws Exception if timeout is reached or connection fails
     */
    void waitForZeroConnections(TargetDatabase database, long timeoutMillis) throws Exception;

    /**
     * Calculates a deterministic checksum of the database data.
     * @param database The target database configuration
     * @return SHA-256 hex string of the data
     * @throws Exception if hashing or dump fails
     */
    String calculateDataChecksum(TargetDatabase database) throws Exception;

    /**
     * Terminates all active connections to the target database.
     * @param database The target database configuration
     * @throws Exception if termination fails
     */
    void terminateActiveConnections(TargetDatabase database) throws Exception;

    /**
     * Executes the restore process for the target database using the provided dump file.
     * @param database The target database configuration
     * @param dumpFilePath The absolute path to the dump file
     * @throws Exception if restore fails
     */
    void executeRestore(TargetDatabase database, String dumpFilePath) throws Exception;
}
