# API Documentation

Base URL: `http://localhost:8080` (local), `https://<railway>.up.railway.app` (prod)  
Frontend proxies `/api` → backend via Vite `proxy`.

All responses envelope: `ApiResponse<T>`

```json
{
  "success": true,
  "message": "Lead created successfully",
  "data": { ... },
  "error": null,
  "timestamp": "2026-08-06T10:00:00"
}
```

Status codes: `200 OK`, `201 Created`, `400 Bad Request` (validation), `404 Not Found`, `500 Internal`, `503 Service Unavailable` (Ollama down).

---

## `POST /api/leads` — Create Lead

Validate: `customerName` required, `phone` required regex `^[6-9]\d{9}$`.

**Request:**

```json
{
  "customerName": "Rajesh Kumar",
  "phone": "9876543210",
  "location": "Sector 150 Noida",
  "propertyType": "Apartment",
  "configuration": "3 BHK",
  "budget": "1.2 Crore",
  "purpose": "Investment",
  "timeline": "6 months",
  "conversationSummary": "Interested in 3 BHK for investment, budget 1.2Cr"
}
```

**cURL:**

```bash
curl -X POST http://localhost:8080/api/leads \
  -H "Content-Type: application/json" \
  -d '{"customerName":"Rajesh Kumar","phone":"9876543210","location":"Sector 150 Noida","configuration":"3 BHK","budget":"1.2 Crore"}'
```

**Response 201:**

```json
{
  "success": true,
  "message": "Lead created successfully",
  "data": {
    "id": 1,
    "customerName": "Rajesh Kumar",
    "phone": "9876543210",
    "location": "Sector 150 Noida",
    "propertyType": "Apartment",
    "configuration": "3 BHK",
    "budget": "1.2 Crore",
    "purpose": "Investment",
    "timeline": "6 months",
    "conversationSummary": "...",
    "createdAt": "2026-08-06T10:00:00",
    "updatedAt": "2026-08-06T10:00:00"
  }
}
```

**Response 400 (validation):**

```json
{
  "success": false,
  "message": "Validation failed",
  "data": { "phone": "Invalid Indian phone number - must be 10 digits starting with 6-9" },
  "error": "VALIDATION_ERROR"
}
```

---

## `GET /api/leads` — List All Leads

**cURL:**

```bash
curl http://localhost:8080/api/leads
```

**Response 200:**

```json
{
  "success": true,
  "message": "Leads fetched successfully",
  "data": [
    { "id": 1, "customerName": "...", "phone": "...", ... },
    { "id": 2, ... }
  ]
}
```

---

## `GET /api/leads/{id}` — Get Lead by ID

**cURL:**

```bash
curl http://localhost:8080/api/leads/1
```

**Response 200:** single lead object  
**Response 404:**

```json
{ "success": false, "message": "Lead not found with id: 99", "error": "NOT_FOUND" }
```

---

## `DELETE /api/leads/{id}` — Delete Lead

**cURL:**

```bash
curl -X DELETE http://localhost:8080/api/leads/1
```

**Response 200:**

```json
{ "success": true, "message": "Lead deleted successfully", "data": null }
```

---

## `POST /api/call-summary` — Generate Summary & Auto-Save Lead

Calls Ollama with conversation history to extract structured JSON and save lead if valid Name+Phone present.

**Request:**

```json
{
  "conversationHistory": [
    { "role": "user", "content": "Hi, I need 3 BHK", "timestamp": "..." },
    { "role": "assistant", "content": "Great! Budget kya hai?" },
    { "role": "user", "content": "1.2 Crore, name Amit, phone 9876543210" }
  ],
  "customerName": "Amit",
  "phone": "9876543210"
}
```

**cURL:**

```bash
curl -X POST http://localhost:8080/api/call-summary \
  -H "Content-Type: application/json" \
  -d '{"conversationHistory":[{"role":"user","content":"Hi, 3 BHK chahiye"},{"role":"assistant","content":"Budget kya hai?"},{"role":"user","content":"1.2 Crore, Amit, 9876543210"}],"customerName":"Amit","phone":"9876543210"}'
```

**Response 200 (saved):**

```json
{
  "success": true,
  "message": "Summary generated and lead saved",
  "data": {
    "summary": "Customer Amit wants 3 BHK budget 1.2Cr ...",
    "lead": { "id": 5, "customerName": "Amit", ... },
    "structuredJson": "{\"customerName\":\"Amit\",\"phone\":\"9876543210\",...}"
  }
}
```

**Response 200 (summary only, insufficient data):**

```json
{
  "success": true,
  "message": "Summary generated (insufficient data to save lead - need valid Name and Phone)",
  "data": { "summary": "...", "lead": null, "structuredJson": "..." }
}
```

---

## `GET /api/health` — Health Check

**cURL:**

```bash
curl http://localhost:8080/api/health
```

**Response 200:**

```json
{
  "success": true,
  "message": "Service is healthy",
  "data": {
    "status": "UP",
    "timestamp": "2026-08-06T10:00:00",
    "service": "Sky Heights Residency AI Voice Agent",
    "version": "1.0.0",
    "ollama": { "status": "UP", "model": "qwen3:latest", "baseUrl": "http://localhost:11434" },
    "database": { "status": "UP", "type": "PostgreSQL/H2" },
    "voice": { "stt": "Whisper (Faster-Whisper)", "tts": "Kokoro TTS (fallback Piper)", "status": "UP" }
  }
}
```

---

## `POST /api/voice/chat` — AI Chat (Ollama Qwen3)

**Request:**

```json
{
  "message": "Mujhe 2 BHK chahiye, budget 85 Lakhs",
  "sessionId": "optional-uuid",
  "language": "hinglish",
  "history": [
    { "role": "user", "content": "Hi" },
    { "role": "assistant", "content": "Namaste! ..." }
  ]
}
```

**cURL:**

```bash
curl -X POST http://localhost:8080/api/voice/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, mujhe 3 BHK chahiye","history":[]}'
```

**Response 200:**

```json
{
  "success": true,
  "message": "Chat successful",
  "data": {
    "response": "Bahut accha! Aapka budget kya hai? Hamare 3 BHK 1.2 Crore me hai ...",
    "sessionId": "sess_123",
    "detectedLanguage": "hinglish",
    "leadReady": false
  }
}
```

**Error 503 (Ollama down):** returns fallback rule-based response with 200 (graceful), or 503 if hard failure:

```json
{ "success": false, "message": "AI service temporarily unavailable", "error": "Ollama unavailable at ..." }
```

---

## `POST /api/voice/tts` — TTS Info

**Request:** `{ "text": "Namaste! Kaise hain aap?" }`  
**Response:** `{ "provider": "kokoro", "fallback": "piper", "note": "TTS handled client-side via Web Speech API; for local Kokoro configure..." }`

**cURL:**

```bash
curl -X POST http://localhost:8080/api/voice/tts -H "Content-Type: application/json" -d '{"text":"Hello"}'
```

---

## `POST /api/voice/stt` — STT Info

No body required. Returns provider info. Real implementation receives `audio/wav` and transcribes via Faster-Whisper.

**cURL:**

```bash
curl -X POST http://localhost:8080/api/voice/stt -H "Content-Type: application/json" -d '{}'
```

---

## Error Examples

**Microphone permission denied (frontend handles, not API):** frontend `voice.ts` catches `not-allowed` and shows banner.

**Ollama timeout:** `503 { "success": false, "message": "AI service temporarily unavailable" }` — frontend falls back to rule-based.

**DB unavailable:** `500 { "success": false, "message": "Internal server error" }` logged via SLF4J.

---

## Postman Collection

Import `docs/postman_collection.json` (if provided) or use cURL above. For automated tests: `mvn test` covers controller validation, `mock-backend` can be hit with same cURLs.

---

## Validation Rules (Backend `LeadRequest`)

- `customerName`: `@NotBlank`
- `phone`: `@NotBlank` + `@Pattern(regexp="^[6-9]\\d{9}$")` (10 digits, starts 6-9, stripped of non-digits)
- Other fields optional but sanitized via `Sanitizer`

---

## CORS

Allowed: `http://localhost:5173`, `http://localhost:3000`, `https://*.vercel.app` (via `WebConfig` `allowedOriginPatterns("*")` for demo). Credentials true, methods `GET,POST,PUT,DELETE,OPTIONS,PATCH`.
