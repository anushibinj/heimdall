package com.heimdall.backend.service;

import com.heimdall.backend.entity.Snapshot;
import com.heimdall.backend.entity.TargetDatabase;
import com.heimdall.backend.provider.DatabaseProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

@Service
public class RestorationService {

    private static final Logger log = LoggerFactory.getLogger(RestorationService.class);

    @Autowired
    private List<DatabaseProvider> providers;

    @Value("${heimdall.backup.dump-dir:./heimdall-data/dumps}")
    private String dumpDir;

    public void restoreSnapshot(Snapshot snapshot) throws Exception {
        TargetDatabase db = snapshot.getTargetDatabase();
        
        DatabaseProvider provider = providers.stream()
                .filter(p -> p.supports(db.getEngine()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No provider found for engine: " + db.getEngine()));

        // 1. Create a temporary safety backup (rollback net)
        File rollbackDir = new File(dumpDir, db.getId().toString());
        if (!rollbackDir.exists()) {
            rollbackDir.mkdirs();
        }
        String rollbackFileName = "rollback_" + System.currentTimeMillis() + ".backup";
        File rollbackFile = new File(rollbackDir, rollbackFileName);
        String rollbackFilePath = rollbackFile.getAbsolutePath();

        log.info("Creating rollback safety backup: {}", rollbackFilePath);
        provider.executeBackup(db, rollbackFilePath);

        try {
            // 2. Terminate active connections
            log.info("Terminating active connections on {}", db.getDbName());
            provider.terminateActiveConnections(db);

            // 3. Execute restore
            log.info("Executing restore from snapshot: {}", snapshot.getFilePath());
            provider.executeRestore(db, snapshot.getFilePath());

            // 4. Keep rollback safety backup on success for future reference
            log.info("Restore successful. Keeping rollback backup for reference: {}", rollbackFilePath);
        } catch (Exception e) {
            log.error("Restore failed. Triggering rollback using safety backup...", e);
            try {
                provider.terminateActiveConnections(db);
                provider.executeRestore(db, rollbackFilePath);
                log.info("Rollback successful.");
            } catch (Exception rollbackException) {
                log.error("CRITICAL: Rollback also failed!", rollbackException);
                e.addSuppressed(rollbackException);
            }
            throw new RuntimeException("Restoration failed, rollback initiated.", e);
        }
    }
}
