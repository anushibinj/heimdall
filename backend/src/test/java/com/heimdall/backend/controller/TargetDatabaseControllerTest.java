package com.heimdall.backend.controller;

import tools.jackson.databind.ObjectMapper;
import com.heimdall.backend.entity.TargetDatabase;
import com.heimdall.backend.provider.DatabaseProvider;
import com.heimdall.backend.repository.TargetDatabaseRepository;
import com.heimdall.backend.scheduler.DatabaseSchedulingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TargetDatabaseController.class)
public class TargetDatabaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TargetDatabaseRepository repository;

    @MockitoBean
    private DatabaseSchedulingService schedulingService;

    @MockitoBean
    private DatabaseProvider provider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllDatabases_returnsList() throws Exception {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/databases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createDatabase_returnsSavedDatabase() throws Exception {
        TargetDatabase db = new TargetDatabase();
        db.setId(UUID.randomUUID());
        db.setName("Test DB");

        when(repository.save(any(TargetDatabase.class))).thenReturn(db);

        mockMvc.perform(post("/api/databases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(db)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test DB"));
    }

    @Test
    void getDatabase_returnsNotFound_whenNotExists() throws Exception {
        when(repository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/databases/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
