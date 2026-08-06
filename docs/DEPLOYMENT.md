# Deployment Guide

## Overview

| Component | Platform | Build | Env |
|-----------|----------|-------|-----|
| Backend (Spring Boot) | Railway | Dockerfile (`eclipse-temurin:21`) | `DATABASE_URL`, `OLLAMA_BASE_URL` |
| Frontend (React Vite) | Vercel | `npm run build` → `dist` | `VITE_API_URL` |
| Database | Railway PostgreSQL | plugin | auto-injected |
| Ollama + Qwen3 | VM / Docker (`ollama/ollama`) | `ollama pull qwen3:latest` | `OLLAMA_MODEL` |
| Alternative | Docker Compose local | `docker-compose up` | - |

---

## Backend → Railway

### 1. Create Railway Project

- Login railway.app → New Project → Deploy from GitHub → select `ronaksorout09dec/PublicView`
- Set **Root Directory**: `backend`
- **Builder**: Dockerfile (detected) or Nixpacks (Maven). If Nixpacks, ensure `mvn package` works.

### 2. Add PostgreSQL

- Railway → New → Database → PostgreSQL → Add to project
- Variables auto-injected as `DATABASE_URL`, `PGHOST` etc. Map to backend env:
  - `DATABASE_URL` = `jdbc:postgresql://host:port/railway` (convert from `postgres://`)
  - `DB_USERNAME` = `${{Postgres.PGUSER}}`
  - `DB_PASSWORD` = `${{Postgres.PGPASSWORD}}`
  - `DB_DRIVER` = `org.postgresql.Driver`
  - `HIBERNATE_DIALECT` = `org.hibernate.dialect.PostgreSQLDialect`

### 3. Environment Variables (Railway → Variables)

```
DATABASE_URL=jdbc:postgresql://containers-us-west-xxx.railway.app:5432/railway
DB_USERNAME=postgres
DB_PASSWORD=xxx
DB_DRIVER=org.postgresql.Driver
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
OLLAMA_BASE_URL=http://<ollama-host>:11434   # if Ollama on same private network or public VM
OLLAMA_MODEL=qwen3:latest
OLLAMA_TIMEOUT=60000
CORS_ALLOWED_ORIGINS=https://<vercel-frontend>.vercel.app,http://localhost:5173
```

### 4. Deploy & Verify

- Push to `main` → Railway auto-deploys
- Logs: `Application started successfully. Sky Heights Residency is ready`
- Verify: `curl https://<railway-url>/api/health` → `{"status":"UP","ollama":{"status":"UP"...}}`
- `curl https://<railway-url>/api/leads` → `[]`

### 5. Health Check (Railway)

Configure healthcheck path: `/api/health` (in `railway.json` or Dockerfile `HEALTHCHECK` already present).

---

## Frontend → Vercel

### 1. Import GitHub

- vercel.com → Add New Project → Import `PublicView`
- **Root Directory**: `frontend`
- **Framework Preset**: Vite
- **Build Command**: `npm run build`
- **Output Directory**: `dist`
- **Install Command**: `npm install`

### 2. Environment

```
VITE_API_URL=https://<railway-backend>.up.railway.app/api
```

*(Vite `proxy` not used in prod; direct to Railway)*

### 3. Deploy

- Push → Vercel builds (tsc + vite) → deploys to `https://<project>.vercel.app`
- Verify: open site, check Health on header shows "System UP"

### 4. Verify End-to-End

- Allow mic → tap mic → speak Hinglish → check AI response → End Call & Save Lead → check `/leads` page → lead appears
- Test Hindi, Hinglish, English each

---

## Ollama + Qwen3

### Local (dev)

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama serve &   # default :11434
ollama pull qwen3:latest        # 4.7GB; for faster: qwen3:4b (2.5GB) or qwen3:1.7b
ollama list
curl http://localhost:11434/api/tags | jq
curl http://localhost:11434/api/chat -d '{"model":"qwen3:latest","messages":[{"role":"user","content":"Hi"}]}'
```

Config: `backend/src/main/resources/application.properties` → `ollama.base-url=http://localhost:11434`, `ollama.model=qwen3:latest` (both env-overridable).

### Docker (for Railway/private network)

`docker-compose.yml` includes:

```yaml
ollama:
  image: ollama/ollama
  ports: ["11434:11434"]
  volumes: [ollama_data:/root/.ollama]
```

After `docker-compose up`:

```bash
docker exec -it skyheights-ollama ollama pull qwen3:latest
docker exec -it skyheights-ollama ollama list
```

Backend connects via `http://ollama:11434` (Docker DNS) → set `OLLAMA_BASE_URL` accordingly.

### Production Ollama (separate VM)

- E.g., EC2 `g4dn.xlarge` with NVIDIA, install Ollama, pull model, open 11434 security group to Railway backend IP, set `OLLAMA_BASE_URL` to VM public IP.
- For scaling: use Ollama behind load balancer, or switch to vLLM.

---

## Docker Compose (Full Local Stack)

```bash
# Requires Docker & 8GB RAM (Qwen3)
docker-compose up --build -d
docker-compose logs -f backend   # watch startup
docker exec -it skyheights-ollama ollama pull qwen3:latest  # if not pulled
curl http://localhost:8080/api/health
curl http://localhost:5173
# Stop:
docker-compose down
docker volume rm publicview_postgres_data  # reset DB
```

Services: `postgres:5432`, `ollama:11434`, `backend:8080`, `frontend:5173→80`.

---

## Manual Without Docker

See README Quick Start (H2 + `mvn spring-boot:run` + `npm run dev`). Mock backend `mock-backend/server.js` can stand in for Java backend for demo (same API, Ollama integration, fallback).

---

## CI / CD

- GitHub → Railway (auto deploy on push to `main`)
- GitHub → Vercel (auto deploy on push)
- For prod: add GitHub Actions `mvn test` + `npm run build` as gates before deploy.

---

## Troubleshooting

| Issue | Fix |
|-------|-----|
| `Ollama unavailable` in health | `ollama serve` not running, or `OLLAMA_BASE_URL` wrong, or firewall. Check `curl $OLLAMA_BASE_URL/api/tags` |
| `Model not found` | `ollama pull qwen3:latest` on Ollama host; verify `OLLAMA_MODEL` matches `ollama list` |
| `DB connection failed` | Check `DATABASE_URL` JDBC format (`jdbc:postgresql://...`), credentials, `pg_isready` |
| `CORS blocked` | Set `CORS_ALLOWED_ORIGINS` to frontend URL, or `WebConfig` uses `allowedOriginPatterns("*")` for demo |
| `Microphone denied` | Browser: lock icon → site settings → allow microphone; use HTTPS (Vercel has) or localhost |
| `SpeechRecognition not supported` | Use Chrome/Edge; Firefox has limited support; fallback to typing |
| `Frontend 404 on refresh` | Vercel `vercel.json` should have `rewrites: [{source:"/(.*)",destination:"/index.html"}]`; nginx.conf already handles |
| `Lead not saved, phone invalid` | Ensure phone 10 digits 6-9; check `call-summary` payload |
