import VoiceAgent from '../components/VoiceAgent';

export default function Home() {
  return (
    <div style={{ padding: '2rem 1.5rem', background: 'var(--bg)', minHeight: 'calc(100vh - 70px)' }}>
      <div style={{ maxWidth: '900px', margin: '0 auto', textAlign: 'center', marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '2.5rem', marginBottom: '0.5rem', color: 'var(--secondary)' }}>
          Live AI Real Estate Calling Agent
        </h1>
        <p style={{ color: 'var(--text-muted)', fontSize: '1.1rem', maxWidth: '700px', margin: '0 auto' }}>
          Experience natural conversations with <strong>Priya</strong>, our AI executive for <strong>Sky Heights Residency, Sector 150 Noida</strong>.
          Speak in <strong>Hindi, Hinglish, or English</strong> — she understands all!
        </p>
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', marginTop: '1.5rem', flexWrap: 'wrap' }}>
          <span style={{ background: 'white', padding: '0.5rem 1rem', borderRadius: '999px', border: '1px solid var(--border)', fontSize: '0.875rem' }}>🏠 2 BHK ₹85 L</span>
          <span style={{ background: 'white', padding: '0.5rem 1rem', borderRadius: '999px', border: '1px solid var(--border)', fontSize: '0.875rem' }}>🏠 3 BHK ₹1.2 Cr</span>
          <span style={{ background: 'white', padding: '0.5rem 1rem', borderRadius: '999px', border: '1px solid var(--border)', fontSize: '0.875rem' }}>🏠 4 BHK ₹1.6 Cr</span>
          <span style={{ background: 'var(--accent-light)', padding: '0.5rem 1rem', borderRadius: '999px', border: '1px solid #fde68a', fontSize: '0.875rem' }}>Possession Dec 2028</span>
        </div>
      </div>
      <VoiceAgent />
      <div style={{ maxWidth: '900px', margin: '2rem auto 0', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1rem' }}>
        <div style={{ background: 'white', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--border)' }}>
          <h4 style={{ marginBottom: '0.5rem' }}>🎙️ How to talk?</h4>
          <ul style={{ color: 'var(--text-muted)', fontSize: '0.9rem', paddingLeft: '1.25rem' }}>
            <li>Tap <strong>🎤 mic</strong> and speak</li>
            <li>Or type in Hindi/Hinglish/English</li>
            <li>Uses Whisper for STT + Kokoro for TTS locally</li>
            <li>Browser fallback: Web Speech API</li>
          </ul>
        </div>
        <div style={{ background: 'white', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--border)' }}>
          <h4 style={{ marginBottom: '0.5rem' }}>🏢 Project Info</h4>
          <ul style={{ color: 'var(--text-muted)', fontSize: '0.9rem', paddingLeft: '1.25rem' }}>
            <li>Sector 150 Noida, Expressway</li>
            <li>Pool, Gym, Club, Kids Area</li>
            <li>Power Backup, 24x7 Security</li>
            <li>Near Metro, Hospitals, Mall</li>
          </ul>
        </div>
        <div style={{ background: 'white', padding: '1.5rem', borderRadius: '12px', border: '1px solid var(--border)' }}>
          <h4 style={{ marginBottom: '0.5rem' }}>🤖 Powered By</h4>
          <ul style={{ color: 'var(--text-muted)', fontSize: '0.9rem', paddingLeft: '1.25rem' }}>
            <li>Ollama + Qwen3 (local LLM)</li>
            <li>Faster-Whisper STT</li>
            <li>Kokoro TTS → Piper fallback</li>
            <li>Spring Boot + PostgreSQL</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
