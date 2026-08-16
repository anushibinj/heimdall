package com.heimdall.backend.service.storage;

import com.heimdall.backend.entity.TargetDatabase;
import com.heimdall.backend.entity.User;
import com.heimdall.backend.repository.SnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StorageQuotaServiceTest {

    @Mock
    private SnapshotRepository snapshotRepository;

    private StorageQuotaService storageQuotaService;

    private TargetDatabase database;
    private User owner;

    @BeforeEach
    void setUp() {
        storageQuotaService = new StorageQuotaService(snapshotRepository, DataSize.ofMegabytes(10));

        owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@example.com");

        database = new TargetDatabase();
        database.setId(UUID.randomUUID());
        database.setCreatedBy(owner);
    }

    @Test
    void allowsUploadWhenUnderQuota() {
        when(snapshotRepository.sumStoredBytesByOwnerId(owner.getId())).thenReturn(DataSize.ofMegabytes(8).toBytes());

        storageQuotaService.assertCanStore(database, DataSize.ofMegabytes(1).toBytes());
    }

    @Test
    void rejectsUploadWhenQuotaWouldBeExceeded() {
        when(snapshotRepository.sumStoredBytesByOwnerId(owner.getId())).thenReturn(DataSize.ofMegabytes(9).toBytes());

        assertThatThrownBy(() -> storageQuotaService.assertCanStore(database, DataSize.ofMegabytes(2).toBytes()))
                .isInstanceOf(StorageQuotaExceededException.class)
                .hasMessageContaining("owner@example.com")
                .hasMessageContaining("10485760");
    }

    @Test
    void usesDatabaseScopeWhenOwnerIsMissing() {
        database.setCreatedBy(null);
        when(snapshotRepository.sumStoredBytesByDatabaseId(database.getId())).thenReturn(DataSize.ofMegabytes(10).toBytes());

        assertThatThrownBy(() -> storageQuotaService.assertCanStore(database, 1L))
                .isInstanceOf(StorageQuotaExceededException.class)
                .hasMessageContaining("database " + database.getId());
    }

    @Test
    void skipsEnforcementWhenLimitIsZero() {
        storageQuotaService = new StorageQuotaService(snapshotRepository, DataSize.ofBytes(0));

        storageQuotaService.assertCanStore(database, Long.MAX_VALUE);

        verifyNoInteractions(snapshotRepository);
    }

    @Test
    void currentUsageAggregatesAllDatabasesForTheSameOwner() {
        when(snapshotRepository.sumStoredBytesByOwnerId(owner.getId())).thenReturn(12345L);

        assertThat(storageQuotaService.currentUsageBytes(database)).isEqualTo(12345L);
    }
}
