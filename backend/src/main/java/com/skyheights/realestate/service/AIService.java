package com.skyheights.realestate.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyheights.realestate.ai.AIProvider;
import com.skyheights.realestate.ai.ConversationManager;
import com.skyheights.realestate.ai.ConversationMessage;
import com.skyheights.realestate.ai.ConversationSession;
import com.skyheights.realestate.dto.LeadResponse;
import com.skyheights.realestate.entity.Lead;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AIService - Only service allowed to call AIProvider.
 * Controllers must never communicate directly with Ollama.
 * Preserves conversation history, handles language detection, lead extraction.
 * Delegates session memory to ConversationManager.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final AIProvider aiProvider;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private final ConversationManager conversationManager;

    public String chat(String sessionId, String userMessage) {
        ConversationSession session = getOrCreateSession(sessionId);
        session.addUserMessage(userMessage);
        log.info("Chat session {} - user: {}", session.getSessionId(), userMessage);

        try {
            String response = aiProvider.generateResponse(session);
            session.addAssistantMessage(response);
            log.info("Chat session {} - assistant: {}", session.getSessionId(), response);
            // Clean JSON block from user-facing response if present - but keep for internal parsing
            return response;
        } catch (Exception e) {
            log.error("AI chat failed for session {}: {}", sessionId, e.getMessage(), e);
            // Graceful fallback - rule-based response
            String fallback = getFallbackResponse(userMessage, session);
            session.addAssistantMessage(fallback);
            return fallback;
        }
    }

    public ConversationSession getOrCreateSession(String sessionId) {
        String prompt = promptService.generateSystemPrompt();
        ConversationSession session = conversationManager.getOrCreate(sessionId, prompt);
        // Ensure systemPrompt is set if newly created
        if (session.getSystemPrompt() == null || session.getSystemPrompt().isBlank()) {
            session.setSystemPrompt(prompt);
        }
        return session;
    }

    public ConversationSession getSession(String sessionId) {
        return conversationManager.getSession(sessionId);
    }

    public String generateLeadSummary(ConversationSession session) {
        try {
            String summaryPrompt = promptService.generateSummaryPrompt(session.getMessages());
            // Create a temporary session for summary generation
            ConversationSession summarySession = ConversationSession.builder()
                    .systemPrompt(summaryPrompt)
                    .messages(session.getMessages())
                    .sessionId("summary-" + session.getSessionId())
                    .build();
            // Use a generic prompt without system flow for extraction
            String result = aiProvider.generateResponse(summarySession.getSystemPrompt(), summarySession.getMessages());
            log.info("Generated summary: {}", result);
            return extractJson(result);
        } catch (Exception e) {
            log.error("Failed to generate summary: {}", e.getMessage(), e);
            return fallbackSummary(session);
        }
    }

    private String extractJson(String text) {
        if (text == null) return "{}";
        // Extract JSON block if wrapped in markdown
        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            String json = text.substring(jsonStart, jsonEnd + 1);
            // Validate JSON
            try {
                objectMapper.readTree(json);
                return json;
            } catch (Exception e) {
                log.warn("Extracted JSON invalid: {}", json);
            }
        }
        return text;
    }

    private String fallbackSummary(ConversationSession session) {
        // Build simple summary from history
        StringBuilder conv = new StringBuilder();
        for (ConversationMessage m : session.getMessages()) {
            conv.append(m.getRole()).append(": ").append(m.getContent()).append("; ");
        }
        java.util.Map<String, String> fallback = java.util.Map.of(
                "customerName", "Unknown",
                "phone", "",
                "location", "Sector 150 Noida",
                "propertyType", "Apartment",
                "configuration", "",
                "budget", "",
                "purpose", "",
                "timeline", "",
                "conversationSummary", conv.length() > 500 ? conv.substring(0, 500) + "..." : conv.toString()
        );
        try {
            return objectMapper.writeValueAsString(fallback);
        } catch (JsonProcessingException e) {
            return "{\"conversationSummary\":\"" + conv.toString().replace("\"", "'") + "\"}";
        }
    }

    private String getFallbackResponse(String userMessage, ConversationSession session) {
        String lower = userMessage.toLowerCase();
        
        // Simple rule-based fallback when Ollama is unavailable
        if (session.size() <= 1) {
            return "Namaste! Main Priya bol rahi hu Sky Heights Residency se, Sector 150 Noida me. Aapko property ke baare me jankari chahiye? Aap buying ke liye dekh rahe hain ya investment ke liye? (Note: AI service temporarily unavailable, using fallback)";
        }
        if (lower.contains("price") || lower.contains("budget") || lower.contains("kitna") || lower.contains("daam") || lower.contains("rate")) {
            return "Hamare yahan 2 BHK ₹85 Lakhs, 3 BHK ₹1.2 Crore, aur 4 BHK ₹1.6 Crore me available hai. Aapka budget kya hai?";
        }
        if (lower.contains("location") || lower.contains("kahan") || lower.contains("sector")) {
            return "Hamara project Sector 150 Noida me hai, Expressway aur Metro ke paas. Hospitals, Schools, Mall sab nearby hain. Aapko kaunsa location pasand hai?";
        }
        if (lower.contains("amenities") || lower.contains("facilities") || lower.contains("suvidha")) {
            return "Hamare project me Swimming Pool, Gym, Club House, Kids Area, Power Backup aur 24x7 Security hai. Possession December 2028 me milega. Aur kuch janna chahenge?";
        }
        if (lower.matches(".*\\d{10}.*") || lower.contains("phone") || lower.contains("number") || lower.contains("contact")) {
            return "Dhanyavaad! Hum aapko jald contact karenge. Kya aap apna naam bhi bata sakte hain?";
        }
        // Generic fallback
        return "Samajh gayi. Aap thoda aur detail me bata sakte hain? Aapko kaunsi configuration pasand hai - 2 BHK, 3 BHK ya 4 BHK?";
    }

    public boolean isAvailable() {
        return aiProvider.isAvailable();
    }

    public String getModelName() {
        return aiProvider.getModelName();
    }

    public void clearSession(String sessionId) {
        conversationManager.clearSession(sessionId);
        log.info("Cleared session {}", sessionId);
    }

    public Lead parseLeadFromJson(String json, String fallbackSummary) {
        try {
            String cleanJson = extractJson(json);
            var node = objectMapper.readTree(cleanJson);
            return Lead.builder()
                    .customerName(node.has("customerName") && !node.get("customerName").isNull() ? node.get("customerName").asText() : null)
                    .phone(node.has("phone") && !node.get("phone").isNull() ? node.get("phone").asText() : null)
                    .location(node.has("location") && !node.get("location").isNull() ? node.get("location").asText() : "Sector 150 Noida")
                    .propertyType(node.has("propertyType") && !node.get("propertyType").isNull() ? node.get("propertyType").asText() : "Apartment")
                    .configuration(node.has("configuration") && !node.get("configuration").isNull() ? node.get("configuration").asText() : null)
                    .budget(node.has("budget") && !node.get("budget").isNull() ? node.get("budget").asText() : null)
                    .purpose(node.has("purpose") && !node.get("purpose").isNull() ? node.get("purpose").asText() : null)
                    .timeline(node.has("timeline") && !node.get("timeline").isNull() ? node.get("timeline").asText() : null)
                    .conversationSummary(node.has("conversationSummary") && !node.get("conversationSummary").isNull() ? node.get("conversationSummary").asText() : fallbackSummary)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse lead JSON: {}", e.getMessage());
            return Lead.builder().conversationSummary(fallbackSummary).build();
        }
    }
}
