package com.heimdall.backend.controller;

import com.heimdall.backend.entity.Snapshot;
import com.heimdall.backend.repository.SnapshotRepository;
import com.heimdall.backend.service.RestorationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/snapshots")
public class SnapshotController {

    @Autowired
    private SnapshotRepository repository;

    @Autowired
    private RestorationService restorationService;

    @GetMapping
    public List<Snapshot> getSnapshots(@RequestParam UUID databaseId) {
        return repository.findAllByTargetDatabaseIdOrderByCreatedAtDesc(databaseId);
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreSnapshot(@PathVariable UUID id) {
        Optional<Snapshot> snapshotOpt = repository.findById(id);
        if (snapshotOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            restorationService.restoreSnapshot(snapshotOpt.get());
            return ResponseEntity.ok().body("{\"success\":true}");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("{\"success\":false, \"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/{id}/log")
    public ResponseEntity<String> getSnapshotLog(@PathVariable UUID id) {
        Optional<Snapshot> snapshotOpt = repository.findById(id);
        if (snapshotOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String log = snapshotOpt.get().getLogOutput();
        return ResponseEntity.ok().body(log == null ? "No logs available." : log);
    }
}
