package com.example.bankcards.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health-check контроллер.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    /**
     * Простой индикатор работоспособности сервиса.
     * @return {"status": "UP"}
     */
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
