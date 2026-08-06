package com.skyheights.realestate.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ConversationSession stores System Prompt + User + Assistant messages.
 * Always send previous conversation to the model. Never lose context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSession {

    private String sessionId;

    @Builder.Default
    private List<ConversationMessage> messages = new ArrayList<>();

    private String systemPrompt;

    private String detectedLanguage; // hindi, hinglish, english

    private long createdAt;
    private long lastActivityAt;

    public static ConversationSession createNew(String systemPrompt) {
        return ConversationSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .systemPrompt(systemPrompt)
                .messages(new ArrayList<>())
                .createdAt(System.currentTimeMillis())
                .lastActivityAt(System.currentTimeMillis())
                .build();
    }

    public void addUserMessage(String content) {
        messages.add(ConversationMessage.user(content));
        lastActivityAt = System.currentTimeMillis();
        autoDetectLanguage(content);
    }

    public void addAssistantMessage(String content) {
        messages.add(ConversationMessage.assistant(content));
        lastActivityAt = System.currentTimeMillis();
    }

    public void addSystemMessage(String content) {
        messages.add(ConversationMessage.system(content));
    }

    private void autoDetectLanguage(String text) {
        if (text == null || text.isBlank()) return;
        String lower = text.toLowerCase();
        // Very simple heuristic - detailed detection done by AIService
        boolean hasHindiChars = text.matches(".*[\\u0900-\\u097F].*");
        boolean hasHinglishMarkers = lower.matches(".*\\b(aap|hai|nahi|kya|acha|bahut|thoda|kitna|kaisa|hume|chahiye|dekhna|lena)\\b.*");

        if (hasHindiChars) {
            detectedLanguage = "hindi";
        } else if (hasHinglishMarkers) {
            detectedLanguage = "hinglish";
        } else if (lower.matches(".*[\\u0900-\\u097F].*")) {
            detectedLanguage = "hindi";
        } else {
            // default to hinglish as it covers most Indian users, but will be refined by AI
            if (detectedLanguage == null) detectedLanguage = "hinglish";
        }
    }

    public List<ConversationMessage> getAllMessagesWithSystem() {
        List<ConversationMessage> all = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            all.add(ConversationMessage.system(systemPrompt));
        }
        all.addAll(messages);
        return all;
    }

    public int size() {
        return messages.size();
    }

    public void clear() {
        messages.clear();
    }
}
