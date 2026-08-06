package com.skyheights.realestate.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.skyheights.realestate.ai.ConversationManager;
import com.skyheights.realestate.ai.ConversationMessage;
import com.skyheights.realestate.ai.ConversationSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final AIService aiService;
    private final PromptService promptService;
    private final ConversationManager conversationManager;

    public ConversationSession startConversation() {
        String prompt = promptService.generateSystemPrompt();
        ConversationSession session = conversationManager.createSession(prompt);
        log.info("Started new conversation: {}", session.getSessionId());
        return session;
    }

    public String continueConversation(String sessionId, String userMessage) {
        return aiService.chat(sessionId, userMessage);
    }

    public String generateSummary(String sessionId) {
        ConversationSession session = aiService.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        return aiService.generateLeadSummary(session);
    }

    public List<ConversationMessage> getHistory(String sessionId) {
        ConversationSession session = aiService.getSession(sessionId);
        if (session == null) return List.of();
        return session.getMessages();
    }
}
