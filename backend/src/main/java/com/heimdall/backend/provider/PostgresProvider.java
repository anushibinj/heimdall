package com.heimdall.backend.provider;

import com.heimdall.backend.entity.TargetDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;

@Component
public class PostgresProvider implements DatabaseProvider {

    @Override
    public boolean supports(String engine) {
        return "POSTGRES".equalsIgnoreCase(engine);
    }

    @Override
    public boolean testConnection(TargetDatabase database) throws Exception {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", 
                database.getHost(), database.getPort(), database.getDbName());
        
        // This requires the PostgreSQL JDBC driver to be in the classpath
        try (Connection conn = DriverManager.getConnection(jdbcUrl, database.getUsername(), database.getPassword())) {
            return conn.isValid(5);
        }
    }

    @Override
    public void executeBackup(TargetDatabase database, String backupFilePath) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "pg_dump",
                "-h", database.getHost(),
                "-p", String.valueOf(database.getPort()),
                "-U", database.getUsername(),
                "-F", "c",
                "-f", backupFilePath,
                database.getDbName()
        );
        
        processBuilder.environment().put("PGPASSWORD", database.getPassword());
        
        Process process = processBuilder.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("pg_dump failed with exit code: " + exitCode);
        }
    }

    @Value("${heimdall.backup.max-retry:10}")
    private int maxRetry;

    @Override
    public void waitForZeroConnections(TargetDatabase database, long timeoutMillis) throws Exception {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", 
                database.getHost(), database.getPort(), database.getDbName());
        
        long startTime = System.currentTimeMillis();
        long checkInterval = 2000; // 2 seconds
        int retryCount = 0;

        try (Connection conn = DriverManager.getConnection(jdbcUrl, database.getUsername(), database.getPassword())) {
            while (System.currentTimeMillis() - startTime < timeoutMillis && retryCount < maxRetry) {
                retryCount++;
                try (var stmt = conn.prepareStatement(
                        "SELECT count(*) FROM pg_stat_activity WHERE datname = ? AND pid <> pg_backend_pid()")) {
                    stmt.setString(1, database.getDbName());
                    try (var rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            int count = rs.getInt(1);
                            if (count == 0) {
                                return; // Success, zero connections
                            }
                        }
                    }
                }
                Thread.sleep(checkInterval);
            }
            throw new RuntimeException("Timeout waiting for zero connections on database: " + database.getDbName());
        }
    }

    @Override
    public String calculateDataChecksum(TargetDatabase database) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "pg_dump",
                "-h", database.getHost(),
                "-p", String.valueOf(database.getPort()),
                "-U", database.getUsername(),
                "--data-only",
                database.getDbName()
        );
        processBuilder.environment().put("PGPASSWORD", database.getPassword());

        Process process = processBuilder.start();
        
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream is = process.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("pg_dump for checksum failed with exit code: " + exitCode);
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
