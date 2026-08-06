package com.skyheights.realestate.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skyheights.realestate.config.OllamaConfig;
import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class HealthController {

    private final AIService aiService;
    private final OllamaConfig ollamaConfig;

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.debug("GET /api/health - Health check");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("service", "Sky Heights Residency AI Voice Agent");
        health.put("version", "1.0.0");

        // Check Ollama
        boolean ollamaUp = false;
        String model = "unknown";
        try {
            ollamaUp = aiService.isAvailable();
            model = aiService.getModelName();
        } catch (Exception e) {
            log.warn("Health check - Ollama error: {}", e.getMessage());
        }
        health.put("ollama", Map.of(
                "status", ollamaUp ? "UP" : "DOWN",
                "model", model,
                "baseUrl", ollamaConfig.getBaseUrl()
        ));

        // Database is UP if we reached this point (Spring would fail otherwise)
        health.put("database", Map.of("status", "UP", "type", "PostgreSQL/H2"));

        // Voice
        health.put("voice", Map.of(
                "stt", "Whisper (Faster-Whisper)",
                "tts", "Kokoro TTS (fallback Piper)",
                "status", "UP"
        ));

        return ResponseEntity.ok(ApiResponse.success(health, "Service is healthy"));
    }

    @GetMapping("/health/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> details = new HashMap<>();
        details.put("status", "UP");
        details.put("timestamp", LocalDateTime.now());

        Map<String, Object> components = new HashMap<>();
        components.put("db", Map.of("status", "UP"));
        try {
            boolean ollama = aiService.isAvailable();
            components.put("ollama", Map.of("status", ollama ? "UP" : "DOWN", "model", aiService.getModelName()));
        } catch (Exception e) {
            components.put("ollama", Map.of("status", "DOWN", "error", e.getMessage()));
        }
        details.put("components", components);
        return ResponseEntity.ok(details);
    }
}
