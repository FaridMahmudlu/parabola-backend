package com.turalabdullayev.parabola_backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping({"/", "/health", "/ping", "/api/v1/health", "/api/v1/ping"})
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("OK");
    }
}
