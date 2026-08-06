package com.skyheights.realestate.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skyheights.realestate.ai.ConversationSession;
import com.skyheights.realestate.dto.ApiResponse;
import com.skyheights.realestate.dto.ChatRequest;
import com.skyheights.realestate.dto.ChatResponse;
import com.skyheights.realestate.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class VoiceController {

    private final AIService aiService;

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody ChatRequest request) {
        log.info("POST /api/voice/chat - session: {}, message: {}", request.getSessionId(), request.getMessage());

        try {
            String sessionId = request.getSessionId();
            ConversationSession session;

            if (sessionId == null || sessionId.isBlank()) {
                session = aiService.getOrCreateSession(null);
                sessionId = session.getSessionId();
                log.info("Created new voice session: {}", sessionId);
            } else {
                session = aiService.getSession(sessionId);
                if (session == null) {
                    session = aiService.getOrCreateSession(sessionId);
                }
            }

            // If history provided and session is empty, populate it
            if (request.getHistory() != null && session.getMessages().isEmpty()) {
                for (ChatRequest.ChatMessageDto msg : request.getHistory()) {
                    if ("user".equals(msg.getRole())) {
                        session.addUserMessage(msg.getContent());
                    } else if ("assistant".equals(msg.getRole())) {
                        session.addAssistantMessage(msg.getContent());
                    }
                }
            }

            String aiResponse = aiService.chat(sessionId, request.getMessage());

            // Detect language simple heuristic
            String detectedLang = detectLanguage(request.getMessage());

            ChatResponse chatResponse = ChatResponse.builder()
                    .response(aiResponse)
                    .sessionId(sessionId != null ? sessionId : session.getSessionId())
                    .detectedLanguage(detectedLang)
                    .leadReady(false)
                    .build();

            // Check if lead can be extracted (if response contains JSON)
            if (aiResponse.contains("\"customerName\"") || aiResponse.contains("\"phone\"")) {
                chatResponse.setLeadReady(true);
            }

            return ResponseEntity.ok(ApiResponse.success(chatResponse, "Chat successful"));

        } catch (Exception e) {
            log.error("Voice chat failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Voice chat failed: " + e.getMessage()));
        }
    }

    @PostMapping("/tts")
    public ResponseEntity<ApiResponse<Map<String, String>>> tts(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        log.info("POST /api/voice/tts - text length: {}", text != null ? text.length() : 0);
        
        // In production, this would call Kokoro TTS / Piper TTS locally
        // For browser fallback, we return the text to be spoken via Web Speech API
        // This endpoint exists to satisfy architecture and allow future local TTS integration
        Map<String, String> response = Map.of(
                "text", text != null ? text : "",
                "provider", "kokoro",
                "fallback", "piper",
                "note", "TTS is handled client-side via Web Speech API; for local Kokoro/Piper, configure voice.tts.provider and ensure service is running"
        );
        return ResponseEntity.ok(ApiResponse.success(response, "TTS info"));
    }

    @PostMapping("/stt")
    public ResponseEntity<ApiResponse<Map<String, String>>> stt(@RequestBody Map<String, String> request) {
        // In production, this would receive audio and transcribe via Faster-Whisper/Whisper.cpp
        // For browser, STT is handled via Web Speech API (webkitSpeechRecognition)
        log.info("POST /api/voice/stt - STT endpoint called");
        Map<String, String> response = Map.of(
                "provider", "faster-whisper",
                "alternative", "whisper.cpp",
                "note", "STT is handled client-side via Web Speech API; for local Whisper, send audio/wav to this endpoint"
        );
        return ResponseEntity.ok(ApiResponse.success(response, "STT info"));
    }

    private String detectLanguage(String text) {
        if (text == null) return "hinglish";
        if (text.matches(".*[\\u0900-\\u097F].*")) return "hindi";
        String lower = text.toLowerCase();
        if (lower.matches(".*\\b(aap|hai|nahi|kya|acha|bahut|thoda|kitna|kaisa|hume|chahiye)\\b.*")) return "hinglish";
        return "english";
    }
}
