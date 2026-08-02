package com.heimdall.backend.provider;

import com.heimdall.backend.entity.TargetDatabase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PostgresProviderTest {

    private final PostgresProvider provider = new PostgresProvider();

    @Test
    public void testSupportsPostgres() {
        assertTrue(provider.supports("POSTGRES"));
        assertTrue(provider.supports("postgres"));
    }

    @Test
    public void testDoesNotSupportOtherEngines() {
        assertFalse(provider.supports("MYSQL"));
        assertFalse(provider.supports("ORACLE"));
        assertFalse(provider.supports(null));
    }
    
    @Test
    public void testExecuteBackupWithInvalidDbThrowsException() {
        TargetDatabase db = new TargetDatabase();
        db.setHost("localhost");
        db.setPort(5432);
        db.setDbName("nonexistent_db");
        db.setUsername("invalid_user");
        db.setPassword("invalid_pass");
        
        // This will likely throw an exception because pg_dump might not be installed,
        // or if it is, the credentials/host will be invalid.
        // We just assert that it throws some exception rather than hanging or succeeding.
        assertThrows(Exception.class, () -> {
            provider.executeBackup(db, "/tmp/dummy.dump");
        });
    }
}
