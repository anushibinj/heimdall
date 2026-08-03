package com.heimdall.backend.repository;

import com.heimdall.backend.entity.Snapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, UUID> {
    Snapshot findFirstByTargetDatabaseIdOrderByCreatedAtDesc(UUID targetDatabaseId);
}
