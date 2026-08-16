package com.heimdall.backend.repository;

import com.heimdall.backend.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, UUID> {
    Snapshot findFirstByTargetDatabaseIdOrderByCreatedAtDesc(UUID targetDatabaseId);
    
    java.util.List<Snapshot> findAllByTargetDatabaseIdOrderByCreatedAtDesc(UUID targetDatabaseId);
    List<Snapshot> findByCreatedAtBefore(LocalDateTime cutoffDate);

    @Query("SELECT COALESCE(SUM(s.fileSizeBytes), 0) FROM Snapshot s WHERE s.status = 'SUCCESS' AND s.targetDatabase.createdBy.id = :userId")
    long sumStoredBytesByOwnerId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(s.fileSizeBytes), 0) FROM Snapshot s WHERE s.status = 'SUCCESS' AND s.targetDatabase.id = :databaseId")
    long sumStoredBytesByDatabaseId(@Param("databaseId") UUID databaseId);
}
