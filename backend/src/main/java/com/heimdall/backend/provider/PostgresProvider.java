package com.heimdall.backend.provider;

import com.heimdall.backend.entity.TargetDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class PostgresProvider implements DatabaseProvider {

    @Value("${heimdall.backup.process-timeout-minutes:60}")
    private long processTimeoutMinutes;

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
        
        boolean finished = process.waitFor(processTimeoutMinutes, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("pg_dump process timed out after " + processTimeoutMinutes + " minutes");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorOutput = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n"));
            throw new RuntimeException("pg_dump failed with exit code: " + exitCode + ". Error: " + errorOutput);
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
        
        boolean finished = process.waitFor(processTimeoutMinutes, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("pg_dump checksum process timed out after " + processTimeoutMinutes + " minutes");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorOutput = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n"));
            throw new RuntimeException("pg_dump for checksum failed with exit code: " + exitCode + ". Error: " + errorOutput);
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public void terminateActiveConnections(TargetDatabase database) throws Exception {
        // Connect to 'postgres' database to safely terminate connections on target database
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/postgres", 
                database.getHost(), database.getPort());
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl, database.getUsername(), database.getPassword())) {
            try (var stmt = conn.prepareStatement(
                    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = ? AND pid <> pg_backend_pid()")) {
                stmt.setString(1, database.getDbName());
                stmt.execute();
            }
        }
    }

    @Override
    public void executeRestore(TargetDatabase database, String dumpFilePath) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                "pg_restore",
                "--clean",
                "--if-exists",
                "-h", database.getHost(),
                "-p", String.valueOf(database.getPort()),
                "-U", database.getUsername(),
                "-d", database.getDbName(),
                dumpFilePath
        );
        processBuilder.environment().put("PGPASSWORD", database.getPassword());

        Process process = processBuilder.start();
        
        boolean finished = process.waitFor(processTimeoutMinutes, TimeUnit.MINUTES);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("pg_restore process timed out after " + processTimeoutMinutes + " minutes");
        }
        
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            String errorOutput = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                    .lines().collect(Collectors.joining("\n"));
            throw new RuntimeException("pg_restore failed with exit code: " + exitCode + ". Error: " + errorOutput);
        }
    }
}
