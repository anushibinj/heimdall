package com.heimdall.backend.entity;

import com.heimdall.backend.repository.SnapshotRepository;
import com.heimdall.backend.repository.TargetDatabaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EntityMappingTest {

    @Autowired
    private TargetDatabaseRepository targetDatabaseRepository;

    @Autowired
    private SnapshotRepository snapshotRepository;

    @Test
    public void testSaveAndRetrieveTargetDatabase() {
        TargetDatabase db = new TargetDatabase();
        db.setName("Test DB");
        db.setEngine("POSTGRES");
        db.setHost("localhost");
        db.setPort(5432);
        db.setDbName("test_db");
        db.setUsername("user");
        db.setPassword("pass");
        db.setCronSchedule("0 0 * * * ?");

        TargetDatabase savedDb = targetDatabaseRepository.save(db);

        assertThat(savedDb.getId()).isNotNull();
        
        TargetDatabase retrievedDb = targetDatabaseRepository.findById(savedDb.getId()).orElse(null);
        assertThat(retrievedDb).isNotNull();
        assertThat(retrievedDb.getName()).isEqualTo("Test DB");
    }

    @Test
    public void testSaveAndRetrieveSnapshot() {
        TargetDatabase db = new TargetDatabase();
        db.setName("Test DB");
        db.setEngine("POSTGRES");
        db.setHost("localhost");
        db.setPort(5432);
        db.setDbName("test_db");
        db.setUsername("user");
        db.setPassword("pass");
        db.setCronSchedule("0 0 * * * ?");
        TargetDatabase savedDb = targetDatabaseRepository.save(db);

        Snapshot snapshot = new Snapshot();
        snapshot.setTargetDatabase(savedDb);
        snapshot.setCreatedAt(LocalDateTime.now());
        snapshot.setFilePath("/tmp/backup.dump");
        snapshot.setFileSizeBytes(1024L);
        snapshot.setChecksum("abc123hash");
        snapshot.setStatus("SUCCESS");

        Snapshot savedSnapshot = snapshotRepository.save(snapshot);

        assertThat(savedSnapshot.getId()).isNotNull();

        Snapshot retrievedSnapshot = snapshotRepository.findById(savedSnapshot.getId()).orElse(null);
        assertThat(retrievedSnapshot).isNotNull();
        assertThat(retrievedSnapshot.getFilePath()).isEqualTo("/tmp/backup.dump");
        assertThat(retrievedSnapshot.getTargetDatabase().getId()).isEqualTo(savedDb.getId());
    }
}
