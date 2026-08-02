package com.heimdall.backend.provider;

import com.heimdall.backend.entity.TargetDatabase;
import org.springframework.stereotype.Component;

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
}
