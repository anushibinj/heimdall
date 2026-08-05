package com.heimdall.backend.scheduler;

import com.heimdall.backend.entity.Snapshot;
import com.heimdall.backend.repository.SnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SnapshotCleanupService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotCleanupService.class);

    private final SnapshotRepository snapshotRepository;

    @Value("${heimdall.snapshot.retention-days:30}")
    private int retentionDays;

    public SnapshotCleanupService(SnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    // Run daily at 1:00 AM
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupOldSnapshots() {
        log.info("Running snapshot cleanup service. Retention days: {}", retentionDays);
        
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        List<Snapshot> oldSnapshots = snapshotRepository.findByCreatedAtBefore(cutoffDate);
        
        int deletedCount = 0;
        for (Snapshot snapshot : oldSnapshots) {
            try {
                Path filePath = Paths.get(snapshot.getFilePath());
                Files.deleteIfExists(filePath);
                snapshotRepository.delete(snapshot);
                deletedCount++;
                log.debug("Deleted old snapshot: {}", snapshot.getId());
            } catch (IOException e) {
                log.error("Failed to delete snapshot file for ID: " + snapshot.getId(), e);
            }
        }
        
        if (deletedCount > 0) {
            log.info("Snapshot cleanup complete. Deleted {} old snapshots.", deletedCount);
        } else {
            log.info("Snapshot cleanup complete. No old snapshots found.");
        }
    }
}
