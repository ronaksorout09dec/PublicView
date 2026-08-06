import { useState, useEffect, useRef } from 'react';
import styles from './VoiceAgent.module.css';
import { useVoice } from '../hooks/useVoice';
import { voiceApi, callSummaryApi } from '../services/api';
import type { ConversationMessage } from '../types';
import { detectLanguage, mapLangToVoiceLang, cleanAIResponse } from '../utils/helpers';

export default function VoiceAgent() {
  const [messages, setMessages] = useState<ConversationMessage[]>([]);
  const [input, setInput] = useState('');
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [language, setLanguage] = useState<'hindi' | 'hinglish' | 'english'>('hinglish');
  const [autoSpeak, setAutoSpeak] = useState(true);
  const [showLeadModal, setShowLeadModal] = useState(false);
  const [leadSaved, setLeadSaved] = useState(false);

  const conversationRef = useRef<HTMLDivElement>(null);
  const voice = useVoice();

  // Auto-scroll
  useEffect(() => {
    if (conversationRef.current) {
      conversationRef.current.scrollTop = conversationRef.current.scrollHeight;
    }
  }, [messages, voice.interimTranscript]);

  // Handle voice transcript
  useEffect(() => {
    if (voice.transcript) {
      setInput(voice.transcript);
      // Auto-send after voice input
      handleSend(voice.transcript);
      voice.setTranscript('');
    }
  }, [voice.transcript]);

  // Greeting on mount
  useEffect(() => {
    const greeting: ConversationMessage = {
      role: 'assistant',
      content: 'Namaste! Main Priya bol rahi hu Sky Heights Residency se, Sector 150 Noida me. Hamara project Expressway aur Metro ke paas hai. Aap property dekh rahe hain ya investment ke liye soch rahe hain? Aap Hindi, Hinglish ya English me baat kar sakte hain!',
      timestamp: new Date().toISOString(),
    };
    setMessages([greeting]);
    if (autoSpeak) {
      setTimeout(() => voice.speak(greeting.content, mapLangToVoiceLang('hinglish')), 500);
    }
  }, []);

  const handleSend = async (text: string = input) => {
    if (!text.trim() || isProcessing) return;

    const detected = detectLanguage(text);
    setLanguage(detected as any);

    const userMsg: ConversationMessage = {
      role: 'user',
      content: text.trim(),
      timestamp: new Date().toISOString(),
    };

    setMessages(prev => [...prev, userMsg]);
    setInput('');
    setIsProcessing(true);
    voice.stopSpeaking();

    try {
      const historyForApi = [...messages, userMsg].map(m => ({
        role: m.role,
        content: m.content
      }));

      const res = await voiceApi.chat({
        message: text.trim(),
        sessionId: sessionId || undefined,
        language: detected,
        history: historyForApi as any,
      });

      if (res.success && res.data) {
        if (!sessionId && res.data.sessionId) {
          setSessionId(res.data.sessionId);
        } else if (res.data.sessionId) {
          setSessionId(res.data.sessionId);
        }

        const cleanResponse = cleanAIResponse(res.data.response);
        const assistantMsg: ConversationMessage = {
          role: 'assistant',
          content: res.data.response, // keep original for lead extraction, but display cleaned
          timestamp: new Date().toISOString(),
        };
        setMessages(prev => [...prev, assistantMsg]);

        if (autoSpeak && cleanResponse) {
          // Detect response language
          const respLang = detectLanguage(cleanResponse);
          voice.speak(cleanResponse, mapLangToVoiceLang(respLang));
        }

        // Check if lead ready (contains JSON)
        if (res.data.leadReady || res.data.response.includes('customerName')) {
          console.log('Lead ready detected');
        }
      } else {
        throw new Error(res.message || 'Failed to get response');
      }
    } catch (err: any) {
      console.error('Chat error:', err);
      const errorMsg: ConversationMessage = {
        role: 'assistant',
        content: err.response?.data?.message || err.message || 'Sorry, kuch technical issue hai. Kya aap dobara bata sakte hain? Ya aap hamare sales team ko direct call kar sakte hain.',
        timestamp: new Date().toISOString(),
      };
      setMessages(prev => [...prev, errorMsg]);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleVoiceToggle = async () => {
    if (voice.isListening) {
      voice.stopListening();
    } else {
      // Check permission
      const hasPermission = await voice.requestMicrophonePermission();
      if (!hasPermission) {
        voice.setError('Microphone permission denied. Please allow in browser settings.');
        return;
      }
      voice.startListening(mapLangToVoiceLang(language));
    }
  };

  const handleEndCall = async () => {
    if (messages.length < 2) return;
    setIsProcessing(true);
    try {
      // Generate summary and save lead
      const res = await callSummaryApi.create(messages);
      if (res.success) {
        setLeadSaved(true);
        const thankYou: ConversationMessage = {
          role: 'assistant',
          content: `Dhanyavaad! Aapki jankari save ho gayi hai. ${res.data.lead ? `Hamari team aapko jald contact karegi. Aapka lead ID: ${res.data.lead.id}` : 'Hum aapko jald contact karenge.'} Sky Heights Residency me interest dikhane ke liye shukriya!`,
          timestamp: new Date().toISOString(),
        };
        setMessages(prev => [...prev, thankYou]);
        if (autoSpeak) voice.speak(cleanAIResponse(thankYou.content), mapLangToVoiceLang(language));
        setShowLeadModal(true);
      }
    } catch (e: any) {
      console.error('End call error:', e);
      alert('Failed to save: ' + (e.response?.data?.message || e.message));
    } finally {
      setIsProcessing(false);
    }
  };

  const handleQuickReply = (text: string) => {
    handleSend(text);
  };

  return (
    <div className={styles.wrapper}>
      <div className={styles.card}>
        <div className={styles.header}>
          <h2>🏢 Sky Heights Residency</h2>
          <p>Sector 150, Noida • Live AI Voice Agent</p>
          <div className={styles.projectBadge}>
            <span>🎙️</span> Priya - Your AI Sales Executive • Hindi • Hinglish • English
          </div>
        </div>

        <div className={styles.conversation} ref={conversationRef}>
          {messages.length === 0 ? (
            <div className={styles.empty}>
              <div className={styles.emptyIcon}>🎙️</div>
              <p>Start a conversation with Priya</p>
              <p style={{ fontSize: '0.875rem', marginTop: '0.5rem' }}>Tap the mic and speak, or type below</p>
            </div>
          ) : (
            messages.map((msg, idx) => (
              <div key={idx} className={`${styles.message} ${styles[msg.role]}`}>
                <div className={styles.role}>{msg.role === 'user' ? 'You' : msg.role === 'assistant' ? 'Priya • Sky Heights' : 'System'}</div>
                <div>{cleanAIResponse(msg.content) || msg.content}</div>
                {idx === messages.length - 1 && msg.role === 'assistant' && messages.length === 1 && (
                  <div className={styles.quickReplies}>
                    <button className={styles.quickReply} onClick={() => handleQuickReply('I want to buy a 3 BHK')}>3 BHK chahiye</button>
                    <button className={styles.quickReply} onClick={() => handleQuickReply('What is the price?')}>Price kya hai?</button>
                    <button className={styles.quickReply} onClick={() => handleQuickReply('Tell me about amenities')}>Amenities batao</button>
                    <button className={styles.quickReply} onClick={() => handleQuickReply('Mujhe investment ke liye chahiye')}>Investment ke liye</button>
                  </div>
                )}
              </div>
            ))
          )}

          {voice.interimTranscript && (
            <div className={styles.interim}>🎤 {voice.interimTranscript}</div>
          )}

          {isProcessing && (
            <div className={styles.typing}>
              <span></span><span></span><span></span>
            </div>
          )}

          {voice.error && (
            <div className={styles.error}>
              <span>⚠️ {voice.error}</span>
              <button onClick={voice.clearError}>Dismiss</button>
            </div>
          )}
        </div>

        <div className={styles.controls}>
          <div className={styles.inputRow}>
            <button
              onClick={handleVoiceToggle}
              className={`${styles.btn} ${styles.btnVoice} ${voice.isListening ? styles.listening : voice.isSpeaking ? styles.speaking : styles.idle}`}
              title={voice.isListening ? 'Stop listening' : 'Start voice input (Whisper)'}
              disabled={isProcessing}
            >
              {voice.isListening ? '⏹️' : voice.isSpeaking ? '🔊' : '🎤'}
            </button>

            <input
              type="text"
              className={styles.textInput}
              placeholder={voice.isListening ? 'Listening... boliye...' : 'Type your message or tap mic to speak...'}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyPress={handleKeyPress}
              disabled={isProcessing || voice.isListening}
            />

            <button
              onClick={() => handleSend()}
              disabled={!input.trim() || isProcessing}
              className={`${styles.btn} ${styles.btnPrimary}`}
            >
              {isProcessing ? '...' : 'Send ➤'}
            </button>
          </div>

          <div className={styles.actions}>
            <button className={styles.actionBtn} onClick={() => setAutoSpeak(!autoSpeak)}>
              {autoSpeak ? '🔊 Voice ON' : '🔇 Voice OFF'}
            </button>
            <button className={styles.actionBtn} onClick={() => voice.stopSpeaking()} disabled={!voice.isSpeaking}>
              Stop Speaking
            </button>
            <button className={styles.actionBtn} onClick={handleEndCall} disabled={messages.length < 2 || isProcessing} style={{ background: leadSaved ? '#dcfce7' : undefined, borderColor: leadSaved ? '#10b981' : undefined }}>
              {leadSaved ? '✅ Saved' : '📞 End Call & Save Lead'}
            </button>
            <button className={styles.actionBtn} onClick={() => { setMessages([]); setSessionId(null); setLeadSaved(false); window.location.reload(); }}>
              🔄 New Call
            </button>
            <div className={styles.language}>
              {(['hinglish', 'hindi', 'english'] as const).map(l => (
                <button key={l} className={`${styles.langBtn} ${language === l ? styles.active : ''}`} onClick={() => setLanguage(l)}>
                  {l === 'hindi' ? 'हिन्दी' : l === 'hinglish' ? 'Hinglish' : 'English'}
                </button>
              ))}
            </div>
          </div>
        </div>

        <div className={styles.statusBar}>
          <div className={styles.statusLeft}>
            <div className={`${styles.dot} ${voice.isListening ? styles.listening : voice.isSpeaking ? styles.speaking : isProcessing ? styles.processing : ''}`} />
            <span>
              {voice.isListening ? 'Listening via Whisper...' : voice.isSpeaking ? 'Speaking via Kokoro...' : isProcessing ? 'Priya is thinking...' : 'Ready • Ollama Qwen3 • Sector 150 Noida'}
            </span>
          </div>
          <span style={{ fontSize: '0.75rem', opacity: 0.7 }}>
            {messages.length} messages • {sessionId ? `Session ${sessionId.slice(0, 8)}` : 'No session'}
          </span>
        </div>
      </div>

      {showLeadModal && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '1rem' }}>
          <div style={{ background: 'white', borderRadius: '16px', padding: '2rem', maxWidth: '500px', width: '100%', textAlign: 'center' }}>
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>✅</div>
            <h3 style={{ marginBottom: '0.5rem' }}>Thank You!</h3>
            <p style={{ color: 'var(--text-muted)', marginBottom: '1.5rem' }}>Aapki jankari save ho gayi hai. Hamari sales team aapko 24 hours me contact karegi. Sky Heights Residency me interest ke liye dhanyavaad!</p>
            <button onClick={() => setShowLeadModal(false)} className={`${styles.btn} ${styles.btnPrimary}`} style={{ width: '100%' }}>Continue Conversation</button>
          </div>
        </div>
      )}
    </div>
  );
}
