package com.heimdall.backend.service.storage;

import com.heimdall.backend.entity.TargetDatabase;
import com.heimdall.backend.entity.User;
import com.heimdall.backend.repository.SnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

/**
 * Enforces a hard cap on stored backup size per application user.
 * Usage is the sum of successful snapshot file sizes for databases owned by that user.
 * Databases without an owner are limited independently using the same cap.
 * A configured limit of 0 disables the quota.
 */
@Service
public class StorageQuotaService {

    private static final Logger log = LoggerFactory.getLogger(StorageQuotaService.class);

    private final SnapshotRepository snapshotRepository;
    private final DataSize maxBytesPerUser;

    public StorageQuotaService(
            SnapshotRepository snapshotRepository,
            @Value("${heimdall.storage.max-bytes-per-user:10GB}") DataSize maxBytesPerUser) {
        this.snapshotRepository = snapshotRepository;
        this.maxBytesPerUser = maxBytesPerUser;
    }

    public void assertCanStore(TargetDatabase database, long additionalBytes) {
        long limitBytes = maxBytesPerUser.toBytes();
        if (limitBytes <= 0) {
            return;
        }

        long usedBytes = currentUsageBytes(database);
        if (usedBytes + additionalBytes > limitBytes) {
            String scope = describeScope(database);
            log.warn("Storage quota exceeded for {}. used={} bytes, additional={} bytes, limit={} bytes",
                    scope, usedBytes, additionalBytes, limitBytes);
            throw new StorageQuotaExceededException(
                    "Storage quota exceeded for " + scope + ": "
                            + (usedBytes + additionalBytes) + " bytes would exceed the limit of "
                            + limitBytes + " bytes (" + maxBytesPerUser + ")");
        }
    }

    long currentUsageBytes(TargetDatabase database) {
        User owner = database.getCreatedBy();
        if (owner != null && owner.getId() != null) {
            return snapshotRepository.sumStoredBytesByOwnerId(owner.getId());
        }
        return snapshotRepository.sumStoredBytesByDatabaseId(database.getId());
    }

    private String describeScope(TargetDatabase database) {
        User owner = database.getCreatedBy();
        if (owner != null && owner.getId() != null) {
            return "user " + (owner.getEmail() != null ? owner.getEmail() : owner.getId());
        }
        return "database " + database.getId();
    }
}
