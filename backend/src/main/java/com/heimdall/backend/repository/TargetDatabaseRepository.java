package com.heimdall.backend.repository;

import com.heimdall.backend.entity.TargetDatabase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TargetDatabaseRepository extends JpaRepository<TargetDatabase, UUID> {
}
