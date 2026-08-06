# Environment Variables Guide

## Backend (`backend/src/main/resources/application.properties`)

All properties support env overrides via `${VAR:default}`.

| Property | Env Var | Default | Required | Description |
|----------|---------|---------|----------|-------------|
| `server.port` | `PORT` | `8080` | - | Spring port (Railway injects) |
| `spring.datasource.url` | `DATABASE_URL` | `jdbc:h2:mem:realestate` | Prod: Yes | JDBC URL |
| `spring.datasource.username` | `DB_USERNAME` | `sa` | Prod: Yes | DB user |
| `spring.datasource.password` | `DB_PASSWORD` | `` | Prod: Yes | DB password |
| `spring.datasource.driver-class-name` | `DB_DRIVER` | `org.h2.Driver` | Prod: Yes | `org.postgresql.Driver` for PG |
| `spring.jpa.properties.hibernate.dialect` | `HIBERNATE_DIALECT` | `H2Dialect` | Prod: Yes | `PostgreSQLDialect` |
| `ollama.base-url` | `OLLAMA_BASE_URL` | `http://localhost:11434` | Yes | Ollama REST URL (never hardcode) |
| `ollama.model` | `OLLAMA_MODEL` | `qwen3:latest` | Yes | Qwen3 model name |
| `ollama.timeout` | `OLLAMA_TIMEOUT` | `60000` | - | ms |
| `ollama.temperature` | `OLLAMA_TEMPERATURE` | `0.7` | - | Sampling |
| `ollama.max-tokens` | `OLLAMA_MAX_TOKENS` | `500` | - | Max predict |
| `cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | - | CORS |
| `voice.stt.provider` | `VOICE_STT_PROVIDER` | `whisper` | - | `whisper` / `faster-whisper` |
| `voice.tts.provider` | `VOICE_TTS_PROVIDER` | `kokoro` | - | `kokoro` |
| `voice.tts.fallback` | `VOICE_TTS_FALLBACK` | `piper` | - | `piper` |
| `project.name` | - | `Sky Heights Residency` | - | Cached |
| `project.location` | - | `Sector 150 Noida` | - | Cached |

**Never hardcode secrets**: use env vars, never log secrets, never expose config via `/api/health` details beyond model name/baseUrl.

**Example `.env` (local):**

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/realestate
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_DRIVER=org.postgresql.Driver
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen3:latest
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

**Production (Railway Variables UI):** set same, `DATABASE_URL` from PostgreSQL plugin (convert `postgres://` → `jdbc:postgresql://`).

**`backend/.env.example` committed as template; real `.env` gitignored.**

---

## Frontend

| Property | Env Var | Default | Description |
|----------|---------|---------|-------------|
| `VITE_API_URL` | `VITE_API_URL` | `/api` | Backend base. Local uses Vite proxy (`/api` → `http://localhost:8080`). Prod: `https://<railway>/api` |

**Local `.env.local` (optional):**

```bash
VITE_API_URL=http://localhost:8080/api
```

**Vercel Env:** `VITE_API_URL=https://skyheights-backend.up.railway.app/api`

No secrets in frontend env (public).

---

## Mock Backend (`mock-backend/server.js`)

| Env | Default |
|-----|---------|
| `PORT` | `8080` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` |
| `OLLAMA_MODEL` | `qwen3:latest` |

```bash
OLLAMA_BASE_URL=http://localhost:11434 OLLAMA_MODEL=qwen3:4b node server.js
```

---

## Ollama

No env, but CLI config:

```bash
OLLAMA_HOST=0.0.0.0:11434 ollama serve   # bind all
OLLAMA_MODELS=/path/to/models           # custom volume
```

---

## Docker Compose

Uses same env via `docker-compose.yml` `environment:` block, with defaults. Postgres credentials hardcoded for demo (`postgres:postgres`), change for prod.

---

## Validation

Backend logs on startup (`log.info("Ollama baseUrl: {}", ollamaConfig.getBaseUrl())`) without logging passwords. Health endpoint exposes only non-sensitive (`model`, `baseUrl`, `status`).

**Check:** `curl http://localhost:8080/api/health | jq .data.ollama` should show `model`, not secrets.
