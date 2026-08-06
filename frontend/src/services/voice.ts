/**
 * Voice Service - Handles STT (Whisper) and TTS (Kokoro/Piper) with browser fallback
 * 
 * Architecture:
 * Browser (Web Speech API) -> Backend -> Whisper -> Qwen3 -> Kokoro -> Audio
 * 
 * For local TTS/STT: Faster-Whisper / Whisper.cpp, Kokoro TTS / Piper TTS
 * For browser fallback: Web Speech API (webkitSpeechRecognition + speechSynthesis)
 */

export type VoiceState = 'idle' | 'listening' | 'processing' | 'speaking' | 'error';

class VoiceService {
  private recognition: SpeechRecognition | null = null;
  private synthesis: SpeechSynthesis | null = null;
  private isListening = false;
  private currentUtterance: SpeechSynthesisUtterance | null = null;

  constructor() {
    if (typeof window !== 'undefined') {
      this.synthesis = window.speechSynthesis;
      this.initRecognition();
    }
  }

  private initRecognition() {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      console.warn('SpeechRecognition not supported in this browser');
      return;
    }
    this.recognition = new SpeechRecognition();
    this.recognition!.continuous = false;
    this.recognition!.interimResults = true;
    this.recognition!.lang = 'hi-IN'; // Default to Hindi - will auto-switch based on detection
    // For Hinglish support, we use hi-IN which handles both Hindi and English mix
  }

  isSTTSupported(): boolean {
    return this.recognition !== null;
  }

  isTTSSupported(): boolean {
    return this.synthesis !== null && 'speechSynthesis' in window;
  }

  /**
   * Start listening via Whisper (browser fallback uses Web Speech API)
   * In production with local Whisper: audio would be sent to backend /api/voice/stt
   */
  startListening(
    onResult: (transcript: string, isFinal: boolean) => void,
    onError: (error: string) => void,
    lang: string = 'hi-IN'
  ): void {
    if (!this.recognition) {
      onError('Speech recognition not supported. Please use Chrome or Edge.');
      return;
    }

    if (this.isListening) {
      this.stopListening();
    }

    this.recognition.lang = this.mapLanguageToBCP47(lang);
    console.log('[Voice] Starting STT with lang:', this.recognition.lang);

    this.recognition.onresult = (event: SpeechRecognitionEvent) => {
      let interim = '';
      let final = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) {
          final += transcript;
        } else {
          interim += transcript;
        }
      }
      if (final) {
        console.log('[Voice STT] Final:', final);
        onResult(final, true);
      } else if (interim) {
        onResult(interim, false);
      }
    };

    this.recognition.onerror = (event: SpeechRecognitionErrorEvent) => {
      console.error('[Voice STT] Error:', event.error);
      this.isListening = false;
      if (event.error === 'not-allowed') {
        onError('Microphone permission denied. Please allow microphone access.');
      } else if (event.error === 'no-speech') {
        onError('No speech detected. Please try again.');
      } else if (event.error !== 'aborted') {
        onError(`Speech recognition error: ${event.error}`);
      }
    };

    this.recognition.onend = () => {
      console.log('[Voice STT] Ended');
      this.isListening = false;
    };

    try {
      this.recognition.start();
      this.isListening = true;
    } catch (e) {
      console.error('[Voice STT] Start failed:', e);
      onError('Failed to start listening');
    }
  }

  stopListening(): void {
    if (this.recognition && this.isListening) {
      try {
        this.recognition.stop();
      } catch (e) {
        console.warn('[Voice STT] Stop error:', e);
      }
      this.isListening = false;
    }
  }

  /**
   * Speak text via Kokoro TTS (browser fallback uses SpeechSynthesis)
   * In production with local Kokoro/Piper: text would be sent to backend /api/voice/tts returning audio
   */
  speak(
    text: string,
    lang: string = 'hi-IN',
    onEnd?: () => void,
    onStart?: () => void
  ): void {
    if (!this.synthesis) {
      console.warn('TTS not supported');
      onEnd?.();
      return;
    }

    // Cancel any ongoing speech
    this.synthesis.cancel();

    // Clean text: remove JSON blocks, markdown
    const cleanText = this.cleanTextForSpeech(text);
    if (!cleanText) {
      onEnd?.();
      return;
    }

    console.log('[Voice TTS] Speaking:', cleanText.substring(0, 100));

    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.lang = this.mapLanguageToBCP47(lang);
    utterance.rate = 0.95;
    utterance.pitch = 1.0;
    utterance.volume = 1.0;

    // Try to find a suitable voice
    const voices = this.synthesis.getVoices();
    const preferredVoice = this.findBestVoice(utterance.lang, voices);
    if (preferredVoice) {
      utterance.voice = preferredVoice;
      console.log('[Voice TTS] Using voice:', preferredVoice.name, preferredVoice.lang);
    }

    utterance.onstart = () => {
      console.log('[Voice TTS] Started');
      onStart?.();
    };
    utterance.onend = () => {
      console.log('[Voice TTS] Ended');
      this.currentUtterance = null;
      onEnd?.();
    };
    utterance.onerror = (e) => {
      console.error('[Voice TTS] Error:', e);
      this.currentUtterance = null;
      onEnd?.();
    };

    this.currentUtterance = utterance;
    this.synthesis.speak(utterance);
  }

  stopSpeaking(): void {
    if (this.synthesis) {
      this.synthesis.cancel();
      this.currentUtterance = null;
    }
  }

  isSpeaking(): boolean {
    return this.synthesis ? this.synthesis.speaking : false;
  }

  private cleanTextForSpeech(text: string): string {
    // Remove JSON blocks
    let clean = text.replace(/```json[\s\S]*?```/g, '');
    clean = clean.replace(/```[\s\S]*?```/g, '');
    clean = clean.replace(/\{[\s\S]*"customerName"[\s\S]*\}/g, '');
    // Remove markdown
    clean = clean.replace(/\*\*/g, '');
    clean = clean.replace(/\*/g, '');
    // Remove extra whitespace
    clean = clean.replace(/\s+/g, ' ').trim();
    return clean;
  }

  private mapLanguageToBCP47(lang: string): string {
    const map: Record<string, string> = {
      'hindi': 'hi-IN',
      'hinglish': 'hi-IN',
      'english': 'en-IN',
      'en': 'en-IN',
      'hi': 'hi-IN',
    };
    return map[lang.toLowerCase()] || 'hi-IN';
  }

  private findBestVoice(lang: string, voices: SpeechSynthesisVoice[]): SpeechSynthesisVoice | null {
    // Prefer Indian voices, then Hindi, then English
    const langLower = lang.toLowerCase();
    
    // Exact match
    let voice = voices.find(v => v.lang.toLowerCase() === langLower);
    if (voice) return voice;

    // Prefix match
    const prefix = langLower.split('-')[0];
    voice = voices.find(v => v.lang.toLowerCase().startsWith(prefix));
    if (voice) return voice;

    // For Hinglish/Hindi, try hi-IN, then en-IN
    if (prefix === 'hi') {
      voice = voices.find(v => v.lang.includes('IN') || v.lang.includes('hi'));
      if (voice) return voice;
    }

    // Fallback to any Indian English voice
    voice = voices.find(v => v.lang.includes('IN'));
    if (voice) return voice;

    // Fallback to first available
    return voices.length > 0 ? voices[0] : null;
  }

  // Check permissions
  async checkMicrophonePermission(): Promise<'granted' | 'denied' | 'prompt'> {
    try {
      const result = await navigator.permissions.query({ name: 'microphone' as PermissionName });
      return result.state as any;
    } catch {
      return 'prompt';
    }
  }

  async requestMicrophonePermission(): Promise<boolean> {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      stream.getTracks().forEach(track => track.stop());
      return true;
    } catch (e) {
      console.error('[Voice] Microphone permission denied:', e);
      return false;
    }
  }
}

export const voiceService = new VoiceService();
export default voiceService;
