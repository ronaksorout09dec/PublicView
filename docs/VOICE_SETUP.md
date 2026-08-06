# Voice Stack Setup

## STT: Faster-Whisper / Whisper.cpp (Local) + Browser Fallback

### Option A: Faster-Whisper (Python, recommended for accuracy)

```bash
pip install faster-whisper
# Model: base (~150MB) or small (484MB) multilingual (supports Hindi)

# Backend integration (pseudo, see `STTService.java` idea):
# POST /api/voice/stt with multipart audio/wav
#   -> faster_whisper.WhisperModel("base", device="cpu", compute_type="int8").transcribe(audio)
#   -> return { "text": "...", "language": "hi" }

# Test:
python -c "from faster_whisper import WhisperModel; m=WhisperModel('base'); segs,_=m.transcribe('sample.wav'); print(''.join(s.text for s in segs))"
```

**Backend endpoint `POST /api/voice/stt` (future):** Accepts `multipart/form-data` `file`, writes temp wav, calls Python subprocess or `faster_whisper`, returns transcript. For now, returns provider info JSON; STT handled client-side.

### Option B: Whisper.cpp (C++, low RAM, no Python)

```bash
git clone https://github.com/ggerganov/whisper.cpp
cd whisper.cpp
make -j
bash ./models/download-ggml-model.sh base  # 142MB multilingual
./build/bin/whisper-cli -m models/ggml-base.bin -f sample.wav --language hi
# HTTP wrapper: use `whisper.cpp` server mode or call via subprocess similarly.
```

### Browser Fallback (current production path)

`frontend/src/services/voice.ts` uses `webkitSpeechRecognition` (Chrome/Edge):

- Lang `hi-IN` for Hindi/Hinglish, `en-IN` for English, auto-switched via `detectLanguage()`
- Handles interim + final, permission errors (`not-allowed`, `no-speech`), `onend`
- Graceful: if `SpeechRecognition` undefined → chat degrades to typing

Test: `voiceService.isSTTSupported()` → false shows typing only.

---

## TTS: Kokoro TTS (Local) + Piper Fallback + Browser

### Option A: Kokoro TTS (82M params, natural, supports Hindi via transliteration)

```bash
pip install kokoro  # or from https://github.com/hexgrad/kokoro
# Also needs espeak-ng, misaki

# Example:
from kokoro import KPipeline
pipeline = KPipeline(lang_code='h')  # h=Hindi, a=English
for _, _, audio in pipeline('नमस्ते, मैं प्रिया बोल रही हूँ', voice='hf_alpha'):
    # audio is 24kHz wav -> return as base64 or file
    pass

# Backend `POST /api/voice/tts {text, lang}` -> calls kokoro -> returns {audioBase64, mime}
# Frontend plays via <audio>
```

**Quality:** Kokoro excels in English; for Hindi, transliterates to Devanagari then generates. For true Hindi voice, Piper may be better.

### Option B: Piper TTS (lightweight, 50+ languages including Hindi)

```bash
pip install piper-tts
# Download Hindi voice:
wget https://huggingface.co/rhasspy/piper-voices/resolve/main/hi/hi_IN-pratham-medium.onnx
wget https://huggingface.co/rhasspy/piper-voices/resolve/main/hi/hi_IN-pratham-medium.onnx.json
echo "नमस्ते" | piper --model hi_IN-pratham-medium.onnx --output_file out.wav

# Backend fallback: if kokoro fails (no GPU, missing model), try piper via subprocess
# Config: `voice.tts.provider=kokoro`, `voice.tts.fallback=piper`
```

**Backend `VoiceController.tts`:** Currently returns provider info; when local TTS installed, it would generate wav and return base64 in `ChatResponse.audioBase64` for frontend `<audio>` play. Frontend `voice.ts` `speak()` would then prefer audio over `speechSynthesis`.

### Browser Fallback (current)

`voiceService.speak(text, lang)`:

- Strips JSON markdown via `cleanTextForSpeech`
- `speechSynthesis.getVoices()`, `findBestVoice(lang)` prefers `hi-IN`, `en-IN`
- `rate 0.95`, `pitch 1.0`, `cancel()` before speak, handles `onstart`/`onend`/`onerror`
- Toggle `autoSpeak` in UI; `stopSpeaking()` interrupts

Test: `voiceService.isTTSSupported()` → false shows text only.

---

## Integration Test

1. **STT accuracy:** Speak "Mujhe Sector 150 me 2 BHK chahiye" with mic → transcript should be near-exact; if Faster-Whisper local, accuracy >95% for Hinglish.
2. **TTS naturalness:** AI response "Bahut accha! Aapka budget kya hai?" → audio should sound human (Kokoro) vs robotic (`speechSynthesis` still acceptable for demo).
3. **Language switch:** Speak Hindi → response Hindi TTS voice (`hi-IN`); speak English → `en-IN`. Verify `voice.ts` `mapLanguageToBCP47`.
4. **Fallback chain:** Kill Kokoro → backend auto fallback to Piper (log `TTS fallback to Piper`); kill Whisper → browser STT still works.

## Latency

- Whisper base on CPU: ~1-2s for 5s audio
- Kokoro on CPU: ~0.5s per sentence
- Ollama Qwen3:4b on CPU: 1-3s; on GPU 0.5s
- Total voice loop: <5s acceptable.

## Production Note

For prod Railway: voice endpoints currently browser-only; to enable local Whisper/Kokoro, deploy backend on VM with models installed (~2GB for base + onnx), or offload to separate voice microservice.
