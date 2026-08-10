package com.heimdall.backend.controller;

import com.heimdall.backend.dto.JobProgressEvent;
import com.heimdall.backend.service.SseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class SseController {

    @Autowired
    private SseService sseService;

    @GetMapping("/subscribe")
    public SseEmitter subscribe() {
        return sseService.subscribe();
    }

    @GetMapping("/active")
    public ResponseEntity<List<JobProgressEvent>> getActiveJobs() {
        return ResponseEntity.ok(sseService.getActiveJobs());
    }
}
