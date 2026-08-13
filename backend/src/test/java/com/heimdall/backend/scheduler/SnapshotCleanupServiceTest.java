package com.heimdall.backend.scheduler;

import com.heimdall.backend.entity.Snapshot;
import com.heimdall.backend.repository.SnapshotRepository;
import com.heimdall.backend.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SnapshotCleanupServiceTest {

    @Mock
    private SnapshotRepository snapshotRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private SnapshotCleanupService cleanupService;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(cleanupService, "retentionDays", 30);
    }

    @Test
    public void testCleanupOldSnapshots() {
        Snapshot oldSnapshot = new Snapshot();
        oldSnapshot.setId(UUID.randomUUID());
        oldSnapshot.setFilePath("databases/test/old_file.backup");
        
        when(snapshotRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(oldSnapshot));
                
        cleanupService.cleanupOldSnapshots();
        
        verify(storageService, times(1)).deleteFile("databases/test/old_file.backup");
        verify(snapshotRepository, times(1)).findByCreatedAtBefore(any(LocalDateTime.class));
        verify(snapshotRepository, times(1)).delete(oldSnapshot);
    }
    
    @Test
    public void testCleanupWithNoOldSnapshots() {
        when(snapshotRepository.findByCreatedAtBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
                
        cleanupService.cleanupOldSnapshots();
        
        verify(storageService, never()).deleteFile(any());
        verify(snapshotRepository, times(1)).findByCreatedAtBefore(any(LocalDateTime.class));
        verify(snapshotRepository, never()).delete(any());
    }
}
