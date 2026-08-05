package com.heimdall.backend.controller;

import com.heimdall.backend.entity.Snapshot;
import com.heimdall.backend.repository.SnapshotRepository;
import com.heimdall.backend.service.RestorationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SnapshotController.class)
public class SnapshotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SnapshotRepository repository;

    @MockitoBean
    private RestorationService restorationService;

    @Test
    void getSnapshots_returnsList() throws Exception {
        UUID dbId = UUID.randomUUID();
        when(repository.findAllByTargetDatabaseIdOrderByCreatedAtDesc(dbId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/snapshots").param("databaseId", dbId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void restoreSnapshot_returnsSuccess() throws Exception {
        UUID snapId = UUID.randomUUID();
        Snapshot snap = new Snapshot();
        snap.setId(snapId);
        
        when(repository.findById(snapId)).thenReturn(Optional.of(snap));
        doNothing().when(restorationService).restoreSnapshot(snap);

        mockMvc.perform(post("/api/snapshots/" + snapId + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
