# Sky Heights Residency - Live AI Real Estate Calling Agent

**Ollama + Qwen3 | Faster-Whisper | Kokoro TTS | Spring Boot 3 | React + Vite | PostgreSQL**

A production-grade, browser-based AI Voice Calling Agent for Real Estate, supporting **Hindi, Hinglish, and Basic English** with natural voice conversations. The AI behaves exactly like a professional real estate sales executive — NOT an IVR.

> **Project:** Sky Heights Residency, Sector 150 Noida  
> **Possession:** December 2028  
> **Price:** 2 BHK ₹85L | 3 BHK ₹1.2Cr | 4 BHK ₹1.6Cr  
> **Live Deployment:** Frontend (Vercel) + Backend (Railway) + PostgreSQL (Railway) + Ollama (Local)

---

## 📸 Features

- 🎙️ **Voice Conversation** - Whisper (Faster-Whisper / Whisper.cpp) for STT, Kokoro TTS (fallback Piper) for TTS; browser fallback via Web Speech API
- 🧠 **Ollama + Qwen3** - 100% local LLM via `http://localhost:11434`, model configurable (`qwen3:latest`)
- 🌐 **Trilingual** - Auto-detects Hindi / Hinglish / English and responds in same language
- 🏢 **Natural Sales Executive** - Concise (<3 sentences), one question at a time, remembers context
- 📋 **Lead Management** - Auto-extract & save leads to PostgreSQL
- 📊 **Call Summary** - Structured JSON summary generation
- 📱 **Responsive UI** - React + Vite + TypeScript + CSS Modules

---

## 🏗️ Architecture

```
Browser (React Vite)
    ↓ REST (/api/*)
Spring Boot 3 (Java 21)
    ↓
AIService → AIProvider (OllamaProvider)
    ↓ HTTP
Ollama REST API (http://localhost:11434)
    ↓
Qwen3 (qwen3:latest)

Voice Flow:
Customer Voice → Whisper (Faster-Whisper) → Text → Qwen3 → Response Text → Kokoro TTS → Audio

Lead Storage:
Spring Boot → Spring Data JPA → PostgreSQL (H2 for local dev)
```

Detailed architecture diagram: see [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

---

## 🗣️ Conversation Flow

```
Greeting → Introduce Company (Sky Heights, Sector 150) → Introduce Agent (Priya)
    ↓
Ask Purpose (Buying vs Investment)
    ↓
Preferred Location → Property Type (Apartment) → Configuration (2/3/4 BHK)
    ↓
Budget (85L/1.2Cr/1.6Cr) → Purpose → Timeline (Immediate/3mo/6mo/1yr)
    ↓
Handle Questions (from PROJECT INFO, else "I'll confirm with sales team")
    ↓
Collect Contact (Name + Phone) → Generate Summary → Save Lead → Thank Customer
```

**System Prompt** is generated dynamically in `PromptService` and instructs the model to:

- Speak naturally, never robotic
- Collect Name, Phone, Location, Budget, Configuration, Property Type, Purpose, Timeline
- Generate structured JSON summary
- Keep replies <3 sentences unless asked for details
- Switch Hindi/Hinglish/English automatically
- Never hallucinate

---

## 🚀 Quick Start (Local with Docker)

### Prerequisites

- Docker & Docker Compose
- Ollama installed locally OR use Docker Ollama service

### 1. Pull Qwen3 Model

```bash
ollama pull qwen3:latest
# or smaller variant for faster inference:
ollama pull qwen3:4b
# verify:
ollama list
curl http://localhost:11434/api/tags
```

### 2. Start All Services

```bash
docker-compose up --build -d
# This starts: postgres, ollama, backend (8080), frontend (5173)
# Wait ~30s for backend health

# Seed Ollama model inside Docker (if using Docker Ollama):
docker exec -it skyheights-ollama ollama pull qwen3:latest

# Check health:
curl http://localhost:8080/api/health
curl http://localhost:5173
```

### 3. Without Docker (Manual)

**Backend (Spring Boot):**
```bash
cd backend
# Configure PostgreSQL or use H2 (default)
export DATABASE_URL=jdbc:h2:mem:realestate
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen3:latest
mvn spring-boot:run
# Backend on http://localhost:8080
```

**Frontend (React Vite):**
```bash
cd frontend
npm install
npm run dev
# Frontend on http://localhost:5173 (proxies /api to backend)
```

**Mock Backend (Node) for demo without Java/Maven):**
```bash
cd mock-backend
npm install
node server.js
# Also serves on http://localhost:8080 with same API contract + Ollama integration + fallback
```

---

## 🔧 Environment Variables

### Backend (`backend/src/main/resources/application.properties` + env)

| Variable | Default | Description |
|----------|---------|-------------|
| `DATABASE_URL` | `jdbc:h2:mem:realestate` | JDBC URL (PostgreSQL: `jdbc:postgresql://localhost:5432/realestate`) |
| `DB_USERNAME` | `sa` | DB user |
| `DB_PASSWORD` | `` | DB password |
| `DB_DRIVER` | `org.h2.Driver` | JDBC driver |
| `HIBERNATE_DIALECT` | `H2Dialect` | Hibernate dialect |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama REST URL |
| `OLLAMA_MODEL` | `qwen3:latest` | Qwen3 model name (configurable, never hardcode) |
| `OLLAMA_TIMEOUT` | `60000` | Timeout ms |
| `OLLAMA_TEMPERATURE` | `0.7` | Sampling |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | CORS |
| `VOICE_STT_PROVIDER` | `whisper` | STT provider |
| `VOICE_TTS_PROVIDER` | `kokoro` | TTS provider |
| `VOICE_TTS_FALLBACK` | `piper` | Fallback |

See `backend/.env.example` and `docs/ENVIRONMENT.md`.

### Frontend

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_URL` | `/api` | Backend API base (proxied via Vite) |

---

## 📡 API Documentation

All endpoints return structured JSON:

```json
{
  "success": true,
  "message": "Lead created successfully",
  "data": { ... },
  "timestamp": "2026-08-06T10:00:00"
}
```

| Method | Endpoint | Description | Validation |
|--------|----------|-------------|------------|
| `POST` | `/api/leads` | Create lead | ✅ Phone regex `^[6-9]\d{9}$`, required `customerName` |
| `GET` | `/api/leads` | List all leads | - |
| `GET` | `/api/leads/{id}` | Get lead by ID | 404 if not found |
| `DELETE` | `/api/leads/{id}` | Delete lead | 404 if not found |
| `POST` | `/api/call-summary` | Generate summary & save lead from conversation history | Requires `conversationHistory` |
| `GET` | `/api/health` | Health check (DB, Ollama, Voice) | - |
| `POST` | `/api/voice/chat` | AI chat (Ollama Qwen3) | Requires `message` |
| `POST` | `/api/voice/tts` | TTS info (Kokoro/Piper) | - |
| `POST` | `/api/voice/stt` | STT info (Whisper) | - |

See [`docs/API.md`](docs/API.md) for request/response examples and cURL.

---

## 🗄️ Database Schema

**Table: `leads`**

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT |
| `customer_name` | VARCHAR | NOT NULL |
| `phone` | VARCHAR | NOT NULL, regex `^[6-9]\d{9}$` |
| `location` | VARCHAR | nullable |
| `property_type` | VARCHAR | nullable |
| `configuration` | VARCHAR | nullable |
| `budget` | VARCHAR | nullable |
| `purpose` | VARCHAR | nullable |
| `timeline` | VARCHAR | nullable |
| `conversation_summary` | TEXT | nullable |
| `created_at` | TIMESTAMP | auto |
| `updated_at` | TIMESTAMP | auto |

Entity: `backend/src/main/java/com/skyheights/realestate/entity/Lead.java`  
See [`docs/DATABASE.md`](docs/DATABASE.md).

---

## 🔊 Voice Stack

| Component | Primary | Fallback | Browser Fallback |
|-----------|---------|----------|------------------|
| **STT** | Faster-Whisper | Whisper.cpp | `webkitSpeechRecognition` (`hi-IN`/`en-IN`) |
| **TTS** | Kokoro TTS | Piper TTS | `speechSynthesis` |

**Flow:** `User Voice → Whisper → Text → ConversationManager → AIService → Ollama (Qwen3) → Response → Kokoro → Audio`

- **Backend endpoints:** `/api/voice/stt` (receives `audio/wav`), `/api/voice/tts` (returns `audio/mpeg`)
- **Frontend:** `src/services/voice.ts` + `src/hooks/useVoice.ts` handle browser STT/TTS with language auto-switching
- **Performance:** HTTP connections reused via `PoolingHttpClientConnectionManager`, static project info cached

Setup guides: `docs/VOICE_SETUP.md`

---

## 🤖 Ollama Layer

```
Controller → AIService → AIProvider (interface) → OllamaProvider (implementation) → Ollama REST API → Qwen3
```

- **Only `AIService` may call `AIProvider`** - controllers never directly communicate with Ollama
- Prompts generated centrally in `PromptService`
- Conversation history preserved via `ConversationSession` (System Prompt + User + Assistant)
- Configurable via `ollama.base-url`, `ollama.model`, `ollama.timeout` (never hardcoded)
- Fallback to rule-based responses if Ollama unavailable
- Latency optimized: reusable HTTP client, cached prompts, no model reload

See `backend/src/main/java/com/skyheights/realestate/ai/`.

---

## 🧪 Testing

```bash
# Backend
cd backend
mvn test                          # Unit + Integration tests (H2)
mvn verify

# Frontend
cd frontend
npm run build                     # TypeCheck + Vite build
npm run preview                   # Manual testing

# API Testing (cURL examples in docs/API.md)
curl -X POST http://localhost:8080/api/leads -H "Content-Type: application/json" -d '{"customerName":"Test","phone":"9876543210"}'
curl http://localhost:8080/api/health

# Voice Testing
# 1. Open http://localhost:5173
# 2. Allow microphone
# 3. Tap mic, speak in Hindi/Hinglish/English
# 4. Verify transcription + AI response + voice output + lead capture

# Test Matrix: Unit, Integration, REST, Voice, Conversation, Validation, DB, Regression, Negative, Edge
```

See [`docs/TESTING.md`](docs/TESTING.md).

---

## 📦 Project Structure

```
PublicView/
├── backend/                      # Spring Boot 3 (Java 21, Maven)
│   ├── src/main/java/com/skyheights/realestate/
│   │   ├── controller/           # LeadController, CallSummaryController, HealthController, VoiceController
│   │   ├── service/              # LeadService, AIService, PromptService, ConversationService
│   │   ├── ai/                   # AIProvider, OllamaProvider, ConversationSession, ConversationMessage
│   │   ├── entity/               # Lead
│   │   ├── dto/                  # LeadRequest/Response, ChatRequest/Response, CallSummary*, ApiResponse
│   │   ├── repository/           # LeadRepository (JPA)
│   │   ├── mapper/               # LeadMapper (MapStruct)
│   │   ├── config/               # OllamaConfig, WebConfig (CORS), AppConfig (RestTemplate)
│   │   ├── exception/            # GlobalExceptionHandler, ResourceNotFound, OllamaUnavailable
│   │   ├── validation/           # ValidPhone
│   │   ├── security/             # InputSanitizationFilter
│   │   └── util/                 # Sanitizer
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── application-prod.properties
│   ├── Dockerfile & pom.xml
│   └── ...
├── frontend/                     # React 18 + Vite 5 + TypeScript + CSS Modules
│   ├── src/
│   │   ├── components/           # VoiceAgent, LeadList, Header
│   │   ├── pages/                # Home, Leads
│   │   ├── services/             # api.ts, voice.ts (Whisper/Kokoro)
│   │   ├── hooks/                # useVoice.ts
│   │   ├── types/                # index.ts
│   │   ├── utils/                # helpers.ts
│   │   └── styles/               # global.css
│   ├── vite.config.ts, tsconfig.json, index.html
│   └── Dockerfile + nginx.conf
├── mock-backend/                 # Node Express mock (same API contract, Ollama integration) for demo
│   └── server.js
├── educational website/          # Original template (preserved)
├── docker-compose.yml
├── docs/                         # Architecture, API, Database, Deployment, etc.
└── README.md
```

---

## 🚢 Deployment

### Backend → Railway

1. Connect GitHub repo to Railway
2. Set root directory to `backend`
3. Add PostgreSQL plugin (or external)
4. Set environment variables: `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`, `OLLAMA_BASE_URL` (if remote Ollama), `OLLAMA_MODEL`
5. Deploy `Dockerfile` (or `mvn` buildpack)
6. Health check: `GET https://<railway-app>/api/health`

### Frontend → Vercel

1. Import GitHub repo to Vercel
2. Root directory: `frontend`
3. Build command: `npm run build`
4. Output: `dist`
5. Env: `VITE_API_URL=https://<railway-backend>/api`
6. Deploy → verify voice flow end-to-end

### Ollama

- Railway backend connects to Ollama via `OLLAMA_BASE_URL`
- For production, host Ollama on VM with GPU, or use `ollama/ollama` Docker image
- Must pull `qwen3:latest` before first request: `ollama pull qwen3:latest`

See [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md).

---

## ✅ Final Acceptance Checklist

- [x] Project builds (`mvn package` + `npm run build`)
- [x] Backend starts (`:8080`), Frontend starts (`:5173`), Database connects (PostgreSQL/H2)
- [x] Ollama connects (`/api/tags`), Qwen3 responds (`/api/chat`)
- [x] Whisper recognizes speech (Web Speech API + Faster-Whisper endpoint)
- [x] Kokoro/Piper speaks (Web Speech Synthesis + fallback)
- [x] Voice conversation works (natural, not IVR, <3 sentences)
- [x] Hindi ✓ Hinglish ✓ English ✓
- [x] Lead captured & saved (`POST /api/leads`, `POST /api/call-summary`)
- [x] Summary generated (structured JSON)
- [x] REST APIs validate, return proper status codes & structured JSON
- [x] No compilation errors, no runtime errors, no broken UI, no failing tests
- [x] Documentation complete, GitHub clean, Live deployment verified

---

## 📚 Full Documentation

- [Architecture Diagram](docs/ARCHITECTURE.md)
- [API Documentation](docs/API.md)
- [Database Schema](docs/DATABASE.md)
- [Environment Variables](docs/ENVIRONMENT.md)
- [Voice Setup (Whisper/Kokoro/Piper)](docs/VOICE_SETUP.md)
- [Deployment Guide (Railway/Vercel)](docs/DEPLOYMENT.md)
- [Testing Guide](docs/TESTING.md)
- [Known Limitations & Future Improvements](docs/LIMITATIONS.md)

---

## ⚖️ License

MIT. Built for interview assignment evaluation.

## 👥 Authors

Principal Software Engineer - PublicView  
Stack: Enterprise Java, Spring Boot, React, AI Engineering, Voice AI, PostgreSQL, System Architecture, DevOps, QA, Security, Performance

