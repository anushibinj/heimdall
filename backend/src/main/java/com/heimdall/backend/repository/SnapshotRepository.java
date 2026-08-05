package com.heimdall.backend.repository;

import com.heimdall.backend.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, UUID> {
    Snapshot findFirstByTargetDatabaseIdOrderByCreatedAtDesc(UUID targetDatabaseId);
    
    java.util.List<Snapshot> findAllByTargetDatabaseIdOrderByCreatedAtDesc(UUID targetDatabaseId);
    List<Snapshot> findByCreatedAtBefore(LocalDateTime cutoffDate);
}
