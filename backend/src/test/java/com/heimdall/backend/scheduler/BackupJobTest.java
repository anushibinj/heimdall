package com.heimdall.backend.scheduler;

import com.heimdall.backend.entity.Snapshot;
import com.heimdall.backend.entity.TargetDatabase;
import com.heimdall.backend.provider.DatabaseProvider;
import com.heimdall.backend.repository.SnapshotRepository;
import com.heimdall.backend.repository.TargetDatabaseRepository;
import com.heimdall.backend.service.SseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BackupJobTest {

    @Mock
    private TargetDatabaseRepository databaseRepository;

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private DatabaseProvider provider;

    @Mock
    private SseService sseService;

    @Mock
    private com.heimdall.backend.service.storage.StorageService storageService;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    @InjectMocks
    private BackupJob backupJob;

    private TargetDatabase db;
    private UUID dbId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(backupJob, "timeoutMillis", 300000L);
        ReflectionTestUtils.setField(backupJob, "dumpDir", "/tmp/dumps");
        ReflectionTestUtils.setField(backupJob, "providers", List.of(provider));
        lenient().when(storageService.uploadFile(anyString(), any(File.class))).thenAnswer(inv -> inv.getArgument(0));

        dbId = UUID.randomUUID();
        db = new TargetDatabase();
        db.setId(dbId);
        db.setEngine("POSTGRES");
        db.setName("Test DB");

        JobDataMap dataMap = new JobDataMap();
        dataMap.put("databaseId", dbId.toString());
        dataMap.put("isForced", false);
        lenient().when(context.getJobDetail()).thenReturn(jobDetail);
        lenient().when(jobDetail.getJobDataMap()).thenReturn(dataMap);
        lenient().when(context.getMergedJobDataMap()).thenReturn(dataMap);
        lenient().when(databaseRepository.findById(dbId)).thenReturn(Optional.of(db));
        lenient().when(provider.supports("POSTGRES")).thenReturn(true);
    }

    @Test
    void testBackupSkippedWhenChecksumMatches() throws Exception {
        when(provider.calculateDataChecksum(db)).thenReturn("hash123");
        
        Snapshot previousSnapshot = new Snapshot();
        previousSnapshot.setStatus("SUCCESS");
        previousSnapshot.setChecksum("hash123");
        
        when(snapshotRepository.findFirstByTargetDatabaseIdOrderByCreatedAtDesc(dbId)).thenReturn(previousSnapshot);
        
        backupJob.execute(context);
        
        verify(provider).waitForZeroConnections(db, 300000L);
        verify(provider, never()).executeBackup(any(), any());
        
        ArgumentCaptor<Snapshot> captor = ArgumentCaptor.forClass(Snapshot.class);
        verify(snapshotRepository).save(captor.capture());
        
        Snapshot saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("SKIPPED");
        assertThat(saved.getChecksum()).isEqualTo("hash123");
    }

    @Test
    void testBackupExecutesWhenChecksumDiffers() throws Exception {
        when(provider.calculateDataChecksum(db)).thenReturn("hash456");
        
        Snapshot previousSnapshot = new Snapshot();
        previousSnapshot.setStatus("SUCCESS");
        previousSnapshot.setChecksum("hash123"); // Different
        
        when(snapshotRepository.findFirstByTargetDatabaseIdOrderByCreatedAtDesc(dbId)).thenReturn(previousSnapshot);
        
        backupJob.execute(context);
        
        verify(provider).waitForZeroConnections(db, 300000L);
        verify(provider).executeBackup(eq(db), anyString());
        
        ArgumentCaptor<Snapshot> captor = ArgumentCaptor.forClass(Snapshot.class);
        verify(snapshotRepository).save(captor.capture());
        
        Snapshot saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getChecksum()).isEqualTo("hash456");
        assertThat(saved.getFilePath()).contains(dbId.toString());
    }

    @Test
    void testBackupExecutesWhenNoPreviousSnapshot() throws Exception {
        when(provider.calculateDataChecksum(db)).thenReturn("hash456");
        when(snapshotRepository.findFirstByTargetDatabaseIdOrderByCreatedAtDesc(dbId)).thenReturn(null);
        
        backupJob.execute(context);
        
        verify(provider).executeBackup(eq(db), anyString());
        
        ArgumentCaptor<Snapshot> captor = ArgumentCaptor.forClass(Snapshot.class);
        verify(snapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void testBackupFailsOnTimeout() throws Exception {
        doThrow(new RuntimeException("Timeout waiting for zero connections on database: test"))
                .when(provider).waitForZeroConnections(db, 300000L);
        
        JobExecutionException exception = assertThrows(JobExecutionException.class, () -> backupJob.execute(context));
        assertThat(exception.getMessage()).contains("Backup failed for database");
        
        ArgumentCaptor<Snapshot> captor = ArgumentCaptor.forClass(Snapshot.class);
        verify(snapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("TIMEOUT");
    }
}
