package com.heimdall.backend.controller;

import com.heimdall.backend.entity.User;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal User principal) {
        logger.info("Received request for /me. Principal: " + principal);
        if (principal == null) {
            logger.warn("Principal is null. Returning 401.");
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(principal);
    }
}
