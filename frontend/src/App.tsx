import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Header from './components/Header';
import Home from './pages/Home';
import Leads from './pages/Leads';

export default function App() {
  return (
    <BrowserRouter>
      <Header />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/leads" element={<Leads />} />
      </Routes>
      <footer style={{ background: 'var(--secondary)', color: 'white', padding: '1.5rem', textAlign: 'center', fontSize: '0.875rem' }}>
        <div>© 2026 Sky Heights Residency • Sector 150 Noida • Possession Dec 2028 • Built with Ollama + Qwen3 • Faster-Whisper • Kokoro TTS</div>
        <div style={{ opacity: 0.7, marginTop: '0.5rem' }}>AI Voice Agent • Hindi • Hinglish • English • <a href="https://github.com" style={{ color: '#38bdf8' }}>GitHub</a></div>
      </footer>
    </BrowserRouter>
  );
}
