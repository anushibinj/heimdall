package com.heimdall.backend.controller;

import com.heimdall.backend.entity.TargetDatabase;
import com.heimdall.backend.provider.DatabaseProvider;
import com.heimdall.backend.repository.TargetDatabaseRepository;
import com.heimdall.backend.scheduler.DatabaseSchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/databases")
public class TargetDatabaseController {

    @Autowired
    private TargetDatabaseRepository repository;

    @Autowired
    private DatabaseSchedulingService schedulingService;

    @Autowired
    private List<DatabaseProvider> providers;

    @GetMapping
    public List<TargetDatabase> getAllDatabases() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TargetDatabase> getDatabase(@PathVariable UUID id) {
        Optional<TargetDatabase> db = repository.findById(id);
        return db.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> createDatabase(@RequestBody TargetDatabase database) {
        try {
            TargetDatabase saved = repository.save(database);
            schedulingService.scheduleDatabaseBackup(saved);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to create database: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> deleteDatabase(@PathVariable UUID id) {
        Optional<TargetDatabase> db = repository.findById(id);
        if (db.isPresent()) {
            try {
                schedulingService.unscheduleDatabaseBackup(db.get());
                repository.delete(db.get());
                return ResponseEntity.ok().build();
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Failed to delete database: " + e.getMessage());
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> updateDatabase(@PathVariable UUID id, @RequestBody TargetDatabase databaseDetails) {
        Optional<TargetDatabase> dbOpt = repository.findById(id);
        if (dbOpt.isPresent()) {
            TargetDatabase db = dbOpt.get();
            db.setName(databaseDetails.getName());
            db.setEngine(databaseDetails.getEngine());
            db.setHost(databaseDetails.getHost());
            db.setPort(databaseDetails.getPort());
            db.setDbName(databaseDetails.getDbName());
            db.setUsername(databaseDetails.getUsername());
            
            if (databaseDetails.getPassword() != null && !databaseDetails.getPassword().trim().isEmpty()) {
                db.setPassword(databaseDetails.getPassword());
            }
            
            db.setCronSchedule(databaseDetails.getCronSchedule());
            
            try {
                TargetDatabase updated = repository.save(db);
                schedulingService.scheduleDatabaseBackup(updated);
                return ResponseEntity.ok(updated);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Failed to update database: " + e.getMessage());
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/snapshot")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> triggerSnapshot(@PathVariable UUID id, @RequestParam(defaultValue = "false") boolean force) {
        Optional<TargetDatabase> db = repository.findById(id);
        if (db.isPresent()) {
            try {
                schedulingService.triggerDatabaseBackup(db.get(), force);
                return ResponseEntity.ok().body("{\"success\":true}");
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("{\"success\":false, \"message\":\"Failed to trigger backup: " + e.getMessage() + "\"}");
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> testConnection(@RequestBody TargetDatabase database) {
        try {
            DatabaseProvider provider = providers.stream()
                .filter(p -> p.supports(database.getEngine()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported engine"));
            
            boolean success = provider.testConnection(database);
            if (success) {
                return ResponseEntity.ok().body("{\"success\":true}");
            } else {
                return ResponseEntity.badRequest().body("{\"success\":false}");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\":false, \"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<?> testExistingConnection(@PathVariable UUID id, @RequestBody TargetDatabase databaseDetails) {
        try {
            Optional<TargetDatabase> dbOpt = repository.findById(id);
            if (!dbOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            TargetDatabase existingDb = dbOpt.get();
            
            TargetDatabase testDb = new TargetDatabase();
            testDb.setEngine(databaseDetails.getEngine());
            testDb.setHost(databaseDetails.getHost());
            testDb.setPort(databaseDetails.getPort());
            testDb.setDbName(databaseDetails.getDbName());
            testDb.setUsername(databaseDetails.getUsername());
            
            if (databaseDetails.getPassword() != null && !databaseDetails.getPassword().trim().isEmpty()) {
                testDb.setPassword(databaseDetails.getPassword());
            } else {
                testDb.setPassword(existingDb.getPassword());
            }

            DatabaseProvider provider = providers.stream()
                .filter(p -> p.supports(testDb.getEngine()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Unsupported engine"));
            
            boolean success = provider.testConnection(testDb);
            if (success) {
                return ResponseEntity.ok().body("{\"success\":true}");
            } else {
                return ResponseEntity.badRequest().body("{\"success\":false}");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"success\":false, \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
