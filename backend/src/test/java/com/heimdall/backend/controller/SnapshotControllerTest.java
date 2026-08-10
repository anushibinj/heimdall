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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

@SpringBootTest
public class SnapshotControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }



    @MockitoBean
    private SnapshotRepository repository;

    @MockitoBean
    private RestorationService restorationService;

    @Test
    void getSnapshots_returnsList() throws Exception {
        UUID dbId = UUID.randomUUID();
        when(repository.findAllByTargetDatabaseIdOrderByCreatedAtDesc(dbId)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/snapshots")
                .param("databaseId", dbId.toString())
                .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
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

        mockMvc.perform(post("/api/snapshots/" + snapId + "/restore")
                .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
