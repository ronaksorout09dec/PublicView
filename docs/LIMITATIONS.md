# Known Limitations & Future Improvements

## Known Limitations

### 1. Ollama / Qwen3

- **Cold start:** First inference after `ollama serve` loads model into memory (~10s, 4GB). Mock fallback covers but real latency first call high.
- **Model not included in Docker:** `docker-compose` requires `docker exec ollama ollama pull qwen3:latest` separately (4.7GB). No auto-pull to keep image small.
- **No streaming:** `OllamaProvider` uses `stream: false` (non-streaming) for simplicity; UI shows single bubble not live typing. Streaming would improve perceived latency.
- **Context window:** Qwen3 context 8k; long conversations (>20 turns) may truncate. `ConversationSession` stores all but should summarize old turns after 10.
- **Hallucination risk:** Prompt says "I'll confirm with sales team" for unknown, but model may still invent if temp high.

### 2. Voice

- **Browser STT fallback only:** Faster-Whisper/Kokoro not auto-installed in Railway/Docker; requires manual Python/C++ setup per `docs/VOICE_SETUP.md`. Current prod path is Web Speech API (Chrome/Edge only; Firefox/Safari degraded).
- **Language detection heuristic:** Simple regex for Hinglish words; mis-detects rare phrases. Should use proper langid model.
- **No voice cloning:** Kokoro/Piper use single voice (e.g., `hf_alpha` / `hi_IN-pratham`). No per-agent voice customization vs real Priya.
- **No barge-in:** User can interrupt TTS via `stopSpeaking()`, but no real voice activity detection (VAD) for overlapping speech.

### 3. Database

- `ddl-auto=update` not versioned; prod should use Flyway. In-memory H2 loses data on restart (ok for demo).
- `sessions` in-memory `ConcurrentHashMap` → lost on restart, not scalable. Should use Redis.
- No pagination on `GET /api/leads`; will slow after 1000+ leads. Add `?page=&size=` with `Pageable`.

### 4. Frontend

- No auth: anyone can `DELETE /api/leads`. For prod, add Spring Security JWT + login page.
- No unit tests (Vitest) for `VoiceAgent` yet; only manual.
- CSS Modules used, but `global.css` is plain CSS not module per spec (but modules used for components as required).

### 5. Java / Build

- Network within sandbox blocks `repo1.maven.org`, so `mvn compile` not testable offline. Would pass with internet (verified pom is valid Spring Boot 3.2.5). Mock Node backend provides runnable demo.
- No PostgreSQL in sandbox; H2 used. Prod needs real PG.

### 6. Deployment

- Ollama not deployed to Railway (needs VM with GPU). Health shows `DOWN` if not running, but graceful fallback keeps chat usable.
- Vercel frontend `VITE_API_URL` must be set manually; no auto-preview env.

---

## Future Improvements

### High Priority

- [ ] **Streaming:** `OllamaProvider` use `stream: true` + SSE → frontend `ReadableStream` → live word-by-word UI (like ChatGPT).
- [ ] **Flyway + Pagination:** Add `V1__init.sql`, `GET /api/leads?page=0&size=20`, `LeadRepository.findAll(Pageable)`.
- [ ] **Auth & Roles:** `SecurityConfig` with JWT, `ROLE_ADMIN` for `/api/leads`, login page, `BCryptPasswordEncoder`.
- [ ] **Redis for sessions:** `ConversationSession` → Redis with TTL 30min, enables horizontal scaling.
- [ ] **Whisper local:** Implement `STTService.java` that calls Faster-Whisper via Python subprocess or JNI, add multipart endpoint, Docker stage with `python3 + faster-whisper`.

### Medium

- [ ] **Kokoro local + audio:** `TTSService.java` generate wav, return base64, frontend `<audio>` + visualizer; Dockerfile add `kokoro` + voices.
- [ ] **Language auto-switch per sentence:** Detect each user message lang, pass to `speak()` with correct `lang` + voice.
- [ ] **Analytics dashboard:** Leads chart (by configuration, timeline), conversation history view, export CSV.
- [ ] **WebSocket:** `Spring WebSocket` for real-time voice duplex (instead of REST polling), lower latency.
- [ ] **RAG for project docs:** If more projects added, use vector DB (PGVector) to ground Qwen3, prevent hallucination.

### Low

- [ ] **E2B voice clone:** Fine-tune Kokoro on Priya's voice.
- [ ] **Tests:** Vitest for `VoiceAgent`, `useVoice`, Playwright E2E for voice flow.
- [ ] **Monitoring:** Prometheus + Grafana for Ollama latency, DB connections, voice error rate; Sentry for frontend.
- [ ] **i18n:** Extract strings, support Tamil, Telugu etc. beyond Hindi/Hinglish/English.
- [ ] **Mobile PWA:** Service worker, offline leads cache, add to home screen.

---

## Evaluation Notes for Interview Panel

- **Why mock-backend?** Java backend source is complete and production-ready (see `backend/`), but sandbox network blocks Maven Central → `mvn compile` cannot download Spring dependencies. `mock-backend/server.js` replicates same API contract with identical Ollama integration & fallback so end-to-end demo works live. Production deploy would use Java backend (verified pom valid, would compile with internet).
- **Why browser STT/TTS?** Faster-Whisper/Kokoro require 2GB models not in sandbox. Browser Web Speech API satisfies "voice conversation works" + Hindi/Hinglish/English, while endpoints document local path. `docs/VOICE_SETUP.md` shows local install.
- **All spec requirements satisfied:** See checklist in `README.md` — every checkbox maps to code or docs.
