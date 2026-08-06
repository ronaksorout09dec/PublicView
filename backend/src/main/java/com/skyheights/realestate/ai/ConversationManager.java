package com.skyheights.realestate.ai;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * ConversationManager - Centralized management of conversation sessions.
 * Stores System Prompt + User Messages + Assistant Messages.
 * Always sends previous conversation to the model. Never loses context.
 * 
 * This component is distinct from AIService (which calls AIProvider).
 * ConversationManager handles memory, AIService handles LLM calls.
 */
@Component
@Slf4j
public class ConversationManager {

    private final Map<String, ConversationSession> sessions = new ConcurrentHashMap<>();

    public ConversationSession createSession(String systemPrompt) {
        ConversationSession session = ConversationSession.createNew(systemPrompt);
        sessions.put(session.getSessionId(), session);
        log.info("ConversationManager: created session {}", session.getSessionId());
        return session;
    }

    public ConversationSession getSession(String sessionId) {
        if (sessionId == null) return null;
        ConversationSession session = sessions.get(sessionId);
        if (session != null) {
            log.debug("ConversationManager: retrieved session {} with {} messages", sessionId, session.size());
        } else {
            log.warn("ConversationManager: session not found {}", sessionId);
        }
        return session;
    }

    public ConversationSession getOrCreate(String sessionId, String systemPrompt) {
        if (sessionId != null && sessions.containsKey(sessionId)) {
            return sessions.get(sessionId);
        }
        ConversationSession session = ConversationSession.createNew(systemPrompt);
        // If caller provided ID, preserve it (for VoiceController history replay)
        if (sessionId != null && !sessionId.isBlank()) {
            // Use provided ID instead of generated one
            session.setSessionId(sessionId);
        }
        sessions.put(session.getSessionId(), session);
        log.info("ConversationManager: getOrCreate -> {}", session.getSessionId());
        return session;
    }

    public void addUserMessage(String sessionId, String content) {
        ConversationSession session = getSession(sessionId);
        if (session != null) {
            session.addUserMessage(content);
            log.debug("ConversationManager: added user message to {}", sessionId);
        }
    }

    public void addAssistantMessage(String sessionId, String content) {
        ConversationSession session = getSession(sessionId);
        if (session != null) {
            session.addAssistantMessage(content);
            log.debug("ConversationManager: added assistant message to {}", sessionId);
        }
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
        log.info("ConversationManager: cleared session {}", sessionId);
    }

    public int activeSessions() {
        return sessions.size();
    }

    public Map<String, ConversationSession> getAllSessions() {
        return Map.copyOf(sessions);
    }
}
