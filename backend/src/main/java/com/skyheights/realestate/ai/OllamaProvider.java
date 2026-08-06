package com.skyheights.realestate.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.config.OllamaConfig;
import com.skyheights.realestate.exception.OllamaUnavailableException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OllamaProvider implements AIProvider {

    private final OllamaConfig ollamaConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public String generateResponse(String systemPrompt, List<ConversationMessage> messages) {
        ConversationSession session = ConversationSession.builder()
                .systemPrompt(systemPrompt)
                .messages(messages != null ? new ArrayList<>(messages) : new ArrayList<>())
                .build();
        return generateResponse(session);
    }

    @Override
    public String generateResponse(ConversationSession session) {
        String url = ollamaConfig.getBaseUrl() + "/api/chat";
        log.info("Calling Ollama at {} with model {}", url, ollamaConfig.getModel());
        log.debug("System prompt length: {}, messages: {}", 
                session.getSystemPrompt() != null ? session.getSystemPrompt().length() : 0,
                session.getMessages().size());

        try {
            List<Map<String, String>> ollamaMessages = new ArrayList<>();

            // System prompt as first message
            if (session.getSystemPrompt() != null && !session.getSystemPrompt().isBlank()) {
                Map<String, String> sys = new HashMap<>();
                sys.put("role", "system");
                sys.put("content", session.getSystemPrompt());
                ollamaMessages.add(sys);
            }

            // Add conversation history
            for (ConversationMessage msg : session.getMessages()) {
                // Skip system messages already added to avoid duplication
                if ("system".equals(msg.getRole()) && session.getSystemPrompt() != null) continue;
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole());
                m.put("content", msg.getContent());
                ollamaMessages.add(m);
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", ollamaConfig.getModel());
            requestBody.put("messages", ollamaMessages);
            requestBody.put("stream", false);
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", ollamaConfig.getTemperature());
            options.put("num_predict", ollamaConfig.getMaxTokens());
            requestBody.put("options", options);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            long start = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            long latency = System.currentTimeMillis() - start;
            log.info("Ollama response latency: {}ms", latency);

            if (response.getBody() == null) {
                throw new OllamaUnavailableException("Empty response from Ollama");
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            
            // Handle both chat and generate formats
            JsonNode messageNode = root.get("message");
            if (messageNode != null && messageNode.get("content") != null) {
                String content = messageNode.get("content").asText();
                log.debug("Ollama response: {}", content);
                if (content == null || content.isBlank()) {
                    throw new OllamaUnavailableException("Empty content from Ollama");
                }
                return content.trim();
            }

            // Fallback for /api/generate format
            JsonNode responseNode = root.get("response");
            if (responseNode != null) {
                return responseNode.asText().trim();
            }

            log.error("Unexpected Ollama response format: {}", response.getBody());
            throw new OllamaUnavailableException("Unexpected response format from Ollama");

        } catch (ResourceAccessException e) {
            log.error("Ollama connection failed: {}", e.getMessage(), e);
            throw new OllamaUnavailableException("Ollama service unavailable at " + ollamaConfig.getBaseUrl() + ". Please ensure Ollama is running with Qwen3 model.");
        } catch (OllamaUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling Ollama: {}", e.getMessage(), e);
            throw new OllamaUnavailableException("Failed to generate AI response: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            String url = ollamaConfig.getBaseUrl() + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Ollama not available: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getModelName() {
        return ollamaConfig.getModel();
    }

    public boolean isModelAvailable() {
        try {
            String url = ollamaConfig.getBaseUrl() + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getBody() == null) return false;
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode models = root.get("models");
            if (models != null && models.isArray()) {
                for (JsonNode model : models) {
                    String name = model.get("name").asText();
                    if (name != null && name.contains(ollamaConfig.getModel().replace(":latest", ""))) {
                        return true;
                    }
                }
            }
            log.warn("Model {} not found in available models", ollamaConfig.getModel());
            return false;
        } catch (Exception e) {
            log.warn("Failed to check model availability: {}", e.getMessage());
            return false;
        }
    }
}
