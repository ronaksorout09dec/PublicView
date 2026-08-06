import { useState, useCallback, useRef, useEffect } from 'react';
import voiceService from '../services/voice';

export type VoiceStatus = 'idle' | 'listening' | 'processing' | 'speaking';

export function useVoice() {
  const [status, setStatus] = useState<VoiceStatus>('idle');
  const [transcript, setTranscript] = useState('');
  const [interimTranscript, setInterimTranscript] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSupported, setIsSupported] = useState(true);
  const listeningRef = useRef(false);

  useEffect(() => {
    setIsSupported(voiceService.isSTTSupported() && voiceService.isTTSSupported());
    // Load voices
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      window.speechSynthesis.getVoices();
      window.speechSynthesis.onvoiceschanged = () => {
        window.speechSynthesis.getVoices();
      };
    }
  }, []);

  const startListening = useCallback((lang: string = 'hi-IN') => {
    setError(null);
    setTranscript('');
    setInterimTranscript('');
    setStatus('listening');
    listeningRef.current = true;

    voiceService.startListening(
      (text, isFinal) => {
        if (isFinal) {
          setTranscript(text);
          setInterimTranscript('');
          setStatus('idle');
          listeningRef.current = false;
        } else {
          setInterimTranscript(text);
        }
      },
      (err) => {
        setError(err);
        setStatus('idle');
        listeningRef.current = false;
      },
      lang
    );
  }, []);

  const stopListening = useCallback(() => {
    voiceService.stopListening();
    setStatus('idle');
    listeningRef.current = false;
  }, []);

  const speak = useCallback((text: string, lang: string = 'hi-IN') => {
    setStatus('speaking');
    setError(null);
    voiceService.speak(
      text,
      lang,
      () => setStatus('idle'),
      () => setStatus('speaking')
    );
  }, []);

  const stopSpeaking = useCallback(() => {
    voiceService.stopSpeaking();
    setStatus('idle');
  }, []);

  const requestMicrophonePermission = useCallback(async () => {
    return voiceService.requestMicrophonePermission();
  }, []);

  const isListening = status === 'listening';
  const isSpeaking = status === 'speaking';

  return {
    status,
    transcript,
    interimTranscript,
    error,
    isSupported,
    isListening,
    isSpeaking,
    startListening,
    stopListening,
    speak,
    stopSpeaking,
    setTranscript,
    setError,
    clearError: () => setError(null),
    requestMicrophonePermission,
  };
}
