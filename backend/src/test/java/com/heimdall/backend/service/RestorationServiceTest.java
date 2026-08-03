package com.heimdall.backend.service;

import com.heimdall.backend.entity.Snapshot;
import com.heimdall.backend.entity.TargetDatabase;
import com.heimdall.backend.provider.DatabaseProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestorationServiceTest {

    @Mock
    private DatabaseProvider provider;

    @InjectMocks
    private RestorationService restorationService;

    private TargetDatabase db;
    private Snapshot snapshot;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(restorationService, "providers", List.of(provider));
        ReflectionTestUtils.setField(restorationService, "dumpDir", "/tmp/dumps");

        db = new TargetDatabase();
        db.setId(UUID.randomUUID());
        db.setEngine("POSTGRES");
        db.setDbName("test_db");

        snapshot = new Snapshot();
        snapshot.setTargetDatabase(db);
        snapshot.setFilePath("/tmp/snapshot.backup");

        lenient().when(provider.supports("POSTGRES")).thenReturn(true);
    }

    @Test
    void testRestoreSnapshotSuccess() throws Exception {
        restorationService.restoreSnapshot(snapshot);

        verify(provider).executeBackup(eq(db), anyString());
        verify(provider).terminateActiveConnections(db);
        verify(provider).executeRestore(db, snapshot.getFilePath());
    }

    @Test
    void testRestoreSnapshotFailureTriggersRollback() throws Exception {
        doThrow(new RuntimeException("Restore failed")).when(provider).executeRestore(db, snapshot.getFilePath());

        assertThrows(RuntimeException.class, () -> restorationService.restoreSnapshot(snapshot));

        verify(provider).executeBackup(eq(db), anyString());
        verify(provider, times(2)).terminateActiveConnections(db);
        verify(provider).executeRestore(db, snapshot.getFilePath());
        verify(provider).executeRestore(eq(db), argThat(path -> path.contains("rollback_")));
    }
}
