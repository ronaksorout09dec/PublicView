# Testing Guide

## Test Pyramid

| Layer | Tool | Command | Coverage |
|-------|------|---------|----------|
| Unit | JUnit 5 + Mockito | `mvn test` | Service, Mapper, Validation |
| Integration | SpringBootTest (H2) | `mvn verify` | Controller + DB + Ollama mock |
| REST API | cURL / Postman | manual | `POST /api/leads` etc. |
| Voice | Browser + `voice.ts` | manual | STT/TTS Hinglish |
| Conversation | Manual flow | manual | Full flow → lead saved |
| Validation | MethodArgumentNotValid | `mvn test` + cURL invalid phone | Regex `^[6-9]\d{9}$` |
| DB | JPA + H2 | `mvn test` | CRUD + queries |
| Regression | `LeadServiceTest` | `mvn test` | Create/fetch/delete |
| Negative | cURL 404, invalid JSON | manual | `GET /api/leads/999` → 404 |
| Edge | Empty history, no mic | manual | `POST /api/call-summary` empty → 400 |

---

## Backend Unit & Integration

```bash
cd backend
mvn test
# Expect:
# - LeadServiceTest.testCreateAndFetchLead PASSED
# - OllamaProviderTest.testPromptGeneration PASSED
# Tests use H2 (no Postgres needed)

mvn test -Dtest=OllamaProviderTest -DfailIfNoTests=false
```

**Sample `LeadServiceTest.java`:** creates lead via `LeadService.createLead()` (valid phone `9876543210`), fetches by id, asserts.

---

## REST API Testing (cURL)

```bash
BASE=http://localhost:8080

# Health
curl $BASE/api/health | jq

# Create valid lead
curl -X POST $BASE/api/leads -H "Content-Type: application/json" \
  -d '{"customerName":"Amit","phone":"9876543210","location":"Sector 150 Noida","configuration":"3 BHK"}' | jq

# Create invalid (should 400)
curl -X POST $BASE/api/leads -H "Content-Type: application/json" \
  -d '{"customerName":"","phone":"123"}' | jq  # expect validation error

# List
curl $BASE/api/leads | jq '.data | length'

# Get one
curl $BASE/api/leads/1 | jq

# Delete
curl -X DELETE $BASE/api/leads/1 | jq

# Negative: get non-existent
curl $BASE/api/leads/999 | jq  # expect 404

# Call summary
curl -X POST $BASE/api/call-summary -H "Content-Type: application/json" \
  -d '{"conversationHistory":[{"role":"user","content":"Hi 3 BHK"},{"role":"assistant","content":"Budget?"},{"role":"user","content":"1.2Cr Amit 9876543210"}],"customerName":"Amit","phone":"9876543210"}' | jq

# Voice chat
curl -X POST $BASE/api/voice/chat -H "Content-Type: application/json" \
  -d '{"message":"Mujhe 2 BHK chahiye"}' | jq '.data.response'

# Health detailed
curl $BASE/api/health | jq '.data.ollama'
```

**Expected:** All return `{"success": true/false, "message": "...", "data": ...}` with proper HTTP codes (`201`, `400`, `404`, `200`).

---

## Voice Testing (Browser)

1. **Setup:** `cd frontend && npm run dev` → `http://localhost:5173`, backend `http://localhost:8080` running, allow mic.
2. **STT:** Tap mic (🎤) → speak **Hindi**: "मुझे दो बीएचके चाहिए" → verify interim appears, final transcript sent, AI replies in Hindi, voice speaks via `speechSynthesis`.
3. **Hinglish:** Say "Mujhe 3 BHK chahiye, budget 1.2 Crore hai" → AI should respond Hinglish, ask next question, keep <3 sentences, one question at a time.
4. **English:** "What is the price for 4 BHK?" → "4 BHK is ₹1.6 Crore, possession December 2028..."
5. **Interrupt:** While AI speaking, tap mic again → should cancel speech and listen (handle interruptions).
6. **Mic denied:** Deny permission → banner "Microphone permission denied. Please allow..." (GlobalException style graceful).
7. **Ollama down:** Stop Ollama (`pkill ollama`), chat still works via fallback rule-based (greeting, price, amenities).
8. **Lead save:** Complete flow: Greeting → Buying → Location Noida → 3 BHK → 1.2Cr → Investment → 6 months → Name "Raj" → Phone "9876543210" → End Call → check `/leads` → lead appears with those fields.
9. **Edge:** Empty input → send disabled. Invalid phone "12345" in flow → AI should ask again. JSON in response → frontend `cleanAIResponse` strips it, voice `cleanTextForSpeech` strips.
10. **Cross-browser:** Chrome/Edge (full support), Firefox (typing fallback), Safari (Web Speech limited) → typing works.

**Automated voice regression:** `voice.ts` unit not needed; manual as above + `useVoice` hook tested via UI.

---

## Conversation Testing (Flow)

| Step | Input (any language) | Expected AI |
|------|----------------------|-------------|
| 1 | (auto greeting) | Greeting + Priya + purpose question |
| 2 | "Investment" / "रहने के लिए" | Ask location |
| 3 | "Sector 150 Noida" | Ask property type (Apartment) |
| 4 | "Apartment" | Ask configuration |
| 5 | "3 BHK" | Ask budget |
| 6 | "1.2 Crore" | Ask purpose (confirm) |
| 7 | "Investment" | Ask timeline |
| 8 | "6 months" | Handle questions or ask contact |
| 9 | "What are amenities?" | List pool, gym... else "I'll confirm with sales team" if unknown |
| 10 | "Raj, 9876543210" | Thank, summary, save |

**Assert:** No repeated question, remembers context (if said 3 BHK earlier, not asked again), never hallucinates (if ask "price of 1 BHK" → "I'll confirm...").

---

## Validation Testing

| Case | Input | Expected |
|------|-------|----------|
| Valid phone | `9876543210` | 201 |
| Invalid short | `123` | 400 `Invalid Indian phone` |
| Invalid start 5 | `5123456789` | 400 |
| With spaces | `98765 43210` | 201 (stripped to 9876543210) |
| Empty name | `{customerName:""}` | 400 |
| Missing history | `POST /call-summary {}` | 400 `Conversation history is required` |
| Invalid JSON | `POST /leads` body `notjson` | 400 `Invalid JSON payload` |

---

## Database Testing

- `ddl-auto=update` creates table; verify `select * from leads;` via H2 console (`http://localhost:8080/h2-console` if enabled) or `curl GET /api/leads`.
- Create 2 leads, list → count 2, delete 1 → count 1.

---

## Performance & Regression

- **Reuse HTTP:** `AppConfig.RestTemplate` uses `PoolingHttpClientConnectionManager(100)`.
- **Cache:** `PromptService.PROJECT_INFO` static.
- **Latency:** Log `Ollama response latency: 1234ms` in `OllamaProvider`; aim <2s for `qwen3:4b`.
- **Regression:** After each feature, run `mvn test` + `npm run build` + manual voice flow; never proceed if broken.

---

## CI Checklist (Quality Gate)

Before next milestone, verify:

- [ ] `mvn compile` no warnings
- [ ] `mvn test` all pass
- [ ] `npm run build` no TS errors
- [ ] `curl /api/health` → UP
- [ ] `curl /api/leads` → 200
- [ ] Frontend loads (`http://localhost:5173`)
- [ ] Voice mic works (Chrome)
- [ ] Lead saved & summary generated
- [ ] No exceptions in logs, no console errors

---

## Known Flaky

- Ollama first call cold start (model load ~10s). Second call faster. Mock fallback covers.
- Web Speech `no-speech` error if silence 5s → retry.
