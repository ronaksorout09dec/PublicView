package com.skyheights.realestate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "ollama")
public class OllamaConfig {
    private String baseUrl = "http://localhost:11434";
    private String model = "qwen3:latest";
    private int timeout = 60000;
    private double temperature = 0.7;
    private int maxTokens = 500;
}
