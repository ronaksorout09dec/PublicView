package com.skyheights.realestate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.skyheights.realestate.config.OllamaConfig;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableConfigurationProperties(OllamaConfig.class)
@Slf4j
public class RealEstateApplication {

    public static void main(String[] args) {
        log.info("Starting Sky Heights Residency - AI Real Estate Voice Agent");
        log.info("Ollama Qwen3 will be used for AI reasoning at http://localhost:11434 (configurable)");
        log.info("Voice: Faster-Whisper STT, Kokoro TTS (fallback Piper)");
        SpringApplication.run(RealEstateApplication.class, args);
        log.info("Application started successfully. Sky Heights Residency is ready to serve.");
        log.info("APIs: POST /api/leads, GET /api/leads, GET /api/leads/{id}, DELETE /api/leads/{id}, POST /api/call-summary, GET /api/health");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info("Shutting down Sky Heights AI Voice Agent")));
    }
}
