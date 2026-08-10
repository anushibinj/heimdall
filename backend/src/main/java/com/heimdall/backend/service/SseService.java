package com.heimdall.backend.service;

import com.heimdall.backend.dto.JobProgressEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {
    
    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    
    // Map to keep track of currently active jobs (IN_PROGRESS)
    private final ConcurrentHashMap<UUID, JobProgressEvent> activeJobs = new ConcurrentHashMap<>();
    
    // List of active SSE emitters
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // infinite timeout
        
        emitters.add(emitter);
        
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        emitter.onError((e) -> {
            emitter.complete();
            emitters.remove(emitter);
        });
        
        return emitter;
    }

    public void sendEvent(JobProgressEvent event) {
        if ("IN_PROGRESS".equals(event.getStatus())) {
            activeJobs.put(event.getDatabaseId(), event);
        } else if ("COMPLETED".equals(event.getStatus()) || "FAILED".equals(event.getStatus()) || "SKIPPED".equals(event.getStatus())) {
            activeJobs.remove(event.getDatabaseId());
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("jobProgress")
                        .data(event));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });
        
        emitters.removeAll(deadEmitters);
    }
    
    public List<JobProgressEvent> getActiveJobs() {
        return new ArrayList<>(activeJobs.values());
    }
}
