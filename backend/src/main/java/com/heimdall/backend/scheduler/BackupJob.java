package com.heimdall.backend.scheduler;

import com.heimdall.backend.entity.Snapshot;
import com.heimdall.backend.entity.TargetDatabase;
import com.heimdall.backend.provider.DatabaseProvider;
import com.heimdall.backend.repository.SnapshotRepository;
import com.heimdall.backend.repository.TargetDatabaseRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class BackupJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(BackupJob.class);

    @Autowired
    private TargetDatabaseRepository databaseRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Autowired
    private List<DatabaseProvider> providers;

    @Value("${heimdall.backup.timeout-millis:300000}")
    private long timeoutMillis;

    @Value("${heimdall.backup.dump-dir:./heimdall-data/dumps}")
    private String dumpDir;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String dbIdStr = context.getJobDetail().getJobDataMap().getString("databaseId");
        if (dbIdStr == null) {
            return;
        }

        UUID dbId = UUID.fromString(dbIdStr);
        TargetDatabase db = databaseRepository.findById(dbId).orElse(null);
        if (db == null) {
            return;
        }

        DatabaseProvider provider = providers.stream()
                .filter(p -> p.supports(db.getEngine()))
                .findFirst()
                .orElseThrow(() -> new JobExecutionException("No provider found for engine: " + db.getEngine()));

        Snapshot newSnapshot = new Snapshot();
        newSnapshot.setTargetDatabase(db);
        newSnapshot.setCreatedAt(LocalDateTime.now());
        
        boolean isForced = context.getMergedJobDataMap().getBooleanValue("isForced");

        try {
            // 1. Wait for zero connections
            if (!isForced) {
                provider.waitForZeroConnections(db, timeoutMillis);
            }

            // 2. Checksum validation
            String newChecksum = provider.calculateDataChecksum(db);
            Snapshot latestSnapshot = snapshotRepository.findFirstByTargetDatabaseIdOrderByCreatedAtDesc(db.getId());
            
            if (latestSnapshot != null && "SUCCESS".equals(latestSnapshot.getStatus()) && newChecksum.equals(latestSnapshot.getChecksum())) {
                log.info("Skipping backup for database {}: Data checksum matches the latest snapshot", db.getName());
                newSnapshot.setStatus("SKIPPED");
                newSnapshot.setChecksum(newChecksum);
                newSnapshot.setFilePath("");
                snapshotRepository.save(newSnapshot);
                return; // Skip backup
            }

            log.info("Starting backup for database {} (ID: {})", db.getName(), db.getId());
            // 3. Execute Backup
            File dir = new File(dumpDir, db.getId().toString());
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = "dump_" + System.currentTimeMillis() + ".backup";
            File backupFile = new File(dir, fileName);
            String backupFilePath = backupFile.getAbsolutePath();

            provider.executeBackup(db, backupFilePath);

            log.info("Successfully completed backup for database {}. File: {}", db.getName(), backupFilePath);

            newSnapshot.setStatus("SUCCESS");
            newSnapshot.setChecksum(newChecksum);
            newSnapshot.setFilePath(backupFilePath);
            newSnapshot.setFileSizeBytes(backupFile.length());
            snapshotRepository.save(newSnapshot);

        } catch (Exception e) {
            newSnapshot.setStatus("FAILED");
            newSnapshot.setFilePath("");
            if (e.getMessage() != null && e.getMessage().contains("Timeout waiting for zero connections")) {
                newSnapshot.setStatus("TIMEOUT");
                log.error("Backup timed out for database {}", db.getName(), e);
            } else {
                log.error("Backup failed for database {}", db.getName(), e);
            }
            snapshotRepository.save(newSnapshot);
            throw new JobExecutionException("Backup failed for database: " + db.getName(), e);
        }
    }
}
