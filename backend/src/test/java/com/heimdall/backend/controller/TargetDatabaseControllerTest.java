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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;

@SpringBootTest
public class TargetDatabaseControllerTest {

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

        mockMvc.perform(get("/api/databases")
                .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
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
                .content(objectMapper.writeValueAsString(db))
                .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test DB"));
    }

    @Test
    void getDatabase_returnsNotFound_whenNotExists() throws Exception {
        when(repository.findById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/databases/" + UUID.randomUUID())
                .with(oauth2Login().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }
}
