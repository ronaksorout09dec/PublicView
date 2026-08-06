# Architecture Diagram & Design

## High-Level System Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                              Browser                                 │
│  React 18 + Vite 5 + TypeScript + CSS Modules + Web Speech API     │
│  - VoiceAgent.tsx (Whisper STT, Kokoro TTS)                         │
│  - LeadList.tsx                                                      │
│  - api.ts (axios) → /api/*                                         │
└─────────────────────────────────┬────────────────────────────────────┘
                                  │ HTTPS / REST JSON
                                  ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Spring Boot 3 (Java 21, Maven)                    │
│                                                                      │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────────────┐  │
│  │  Controller  │→→→│   Service    │→→→│      Repository (JPA)    │  │
│  │ - LeadCtrl   │   │ - LeadSvc    │   │   - LeadRepository       │  │
│  │ - VoiceCtrl  │   │ - AIService  │   │         ↕                │  │
│  │ - HealthCtrl │   │ - PromptSvc  │   │   ┌──────────────┐       │  │
│  │ - SummaryCtrl│   │ - ConvSvc    │   │   │  PostgreSQL  │       │  │
│  └──────────────┘   └──────┬───────┘   │   │  (H2 fallback)│       │  │
│                            │           │   └──────────────┘       │  │
│                            ▼           └──────────────────────────┘  │
│                     ┌──────────────┐                                 │
│                     │ AIProvider   │  Interface: Only AIService      │
│                     │ (abstraction)│  may call AIProvider.            │
│                     └──────┬───────┘  Controllers NEVER call Ollama. │
│                            │                                         │
│                     ┌──────▼───────┐                                 │
│                     │OllamaProvider│  HttpComponentsClient (pool)    │
│                     │  - RestTemplate (reused)                       │
│                     │  - /api/chat → Qwen3                           │
│                     └──────┬───────┘                                 │
└────────────────────────────┼──────────────────────────────────────────┘
                             │ HTTP POST /api/chat
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                    Ollama REST API (:11434)                          │
│                    Qwen3 (qwen3:latest)                              │
│  - Base URL configurable: ollama.base-url                            │
│  - Model configurable: ollama.model                                  │
│  - Timeout: ollama.timeout                                           │
│  - Never hardcoded                                                   │
└──────────────────────────────────────────────────────────────────────┘

Lead Entity flow: Frontend → POST /api/leads (validated) → LeadService → LeadRepository → PostgreSQL
Summary flow: Frontend conversation History → POST /api/call-summary → AIService.generateLeadSummary() → Ollama → JSON parse → Lead → Save
```

## Voice Pipeline (Detailed)

```
Customer Voice (mic)
    ↓
[STT] Faster-Whisper / Whisper.cpp   ──→ [Fallback] Web Speech API (webkitSpeechRecognition, lang: hi-IN/en-IN)
    ↓ Text
ConversationManager (ConversationSession: System Prompt + History)
    ↓
AIService → OllamaProvider → Ollama /api/chat (Qwen3, temp 0.7, reuse HTTP conns)
    ↓ Response Text (cleaned, <3 sentences, Hindi/Hinglish/English)
[TTS] Kokoro TTS → Piper TTS fallback  ──→ [Fallback] SpeechSynthesis (voices hi-IN/en-IN)
    ↓ Audio
Browser Audio Output

Caching: Static PROJECT_INFO cached in PromptService, HTTP connections pooled (PoolingHttpClientConnectionManager, 100 total, 20/route)
```

## Conversation Memory

```java
class ConversationSession {
  String sessionId;          // UUID
  String systemPrompt;       // Dynamically generated (PromptService)
  List<ConversationMessage> messages; // user/assistant
  String detectedLanguage;   // hindi/hinglish/english
  long createdAt, lastActivityAt;
}
// Always send previous conversation to model. Never lose context.
// Stored in-memory (ConcurrentHashMap) for demo; production: Redis/DB.
```

## Component Responsibilities

| Layer | Responsibility | Constraint |
|-------|----------------|------------|
| Controller | Validate input, return proper status JSON, delegate to Service | No business logic, no SQL |
| Service | Business logic, AI orchestration, mapping | Single source of truth |
| AIProvider | Abstract LLM provider | Only AIService may call it |
| OllamaProvider | HTTP to Ollama REST, error handling, latency logging | Reuse connections, no model reload |
| PromptService | Dynamic system prompt, project info cache | Centralized, no duplicates |
| Repository | JPA queries | No business logic |
| Entity | JPA + Validation | DB schema source |
| Mapper | DTO ↔ Entity (MapStruct) | - |
| Config | OllamaConfig (@ConfigurationProperties), WebConfig (CORS) | Env vars |
| Exception | GlobalExceptionHandler (logs via SLF4J) | Handles Ollama, DB, Validation, etc. |

## Security & Performance

- **Security:** No hardcoded secrets (env vars), CORS properly via WebConfig + InputSanitizationFilter, sanitize input (Sanitizer), validate phone regex, never log secrets, never expose config
- **Performance:** Reuse HTTP connections (PoolingHttpClientConnectionManager), cache static project info, avoid duplicate prompt generation, never reload model unnecessarily, keep inference latency low (log latency)
- **Logging:** SLF4J on startup, shutdown, API requests, DB errors, Ollama errors, voice errors
- **Error Handling:** GlobalExceptionHandler covers microphone denied, Ollama unavailable, DB unavailable, timeout, invalid JSON, empty response, connection failure, STT/TTS failure, gracefully recovers with fallback rule-based AI

## Deployment Diagram

```
GitHub (PublicView)
  ├── backend/ → Railway (Dockerfile, PostgreSQL plugin, env: DATABASE_URL, OLLAMA_BASE_URL)
  ├── frontend/ → Vercel (Vite build, env: VITE_API_URL)
  └── ollama (separate VM/Docker, volume: ollama_data, pull qwen3:latest)
```

## Data Flow Example

1. User taps mic, says "Mujhe 3 BHK chahiye"
2. Frontend: voice.ts `startListening()` → Web Speech API → transcript "Mujhe 3 BHK chahiye"
3. Frontend: POST /api/voice/chat {message, sessionId, history}
4. Backend: VoiceController → AIService.chat() → session.addUserMessage() → OllamaProvider.generateResponse(session) → POST http://ollama:11434/api/chat
5. Ollama: Qwen3 generates Hinglish response: "Bahut accha! Aapka budget kya hai?"
6. Backend: session.addAssistantMessage(response) → return ChatResponse
7. Frontend: display + voice.ts speak() → Kokoro/Web Speech → audio
8. User ends call → POST /api/call-summary {history} → AIService.generateLeadSummary() → JSON → LeadRepository.save() → return Lead
9. Lead appears in GET /api/leads → LeadList.tsx table

## Tech Stack Choices

- **Java 21 + Spring Boot 3.2.5**: LTS, virtual threads ready, strong validation, JPA
- **PostgreSQL**: Production-grade, JSON not needed; H2 for local dev without setup
- **React 18 + Vite 5**: Fast HMR, modern TS, CSS Modules for isolation
- **Ollama + Qwen3**: Local, private, no cloud cost, configurable; REST API simple
- **Whisper/Kokoro**: Local STT/TTS, privacy; browser fallback ensures demo works without local install
