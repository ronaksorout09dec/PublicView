export function formatPhone(phone: string): string {
  const clean = phone.replace(/\D/g, '');
  if (clean.length === 10) {
    return `${clean.slice(0, 5)} ${clean.slice(5)}`;
  }
  return phone;
}

export function isValidPhone(phone: string): boolean {
  const clean = phone.replace(/\D/g, '');
  return /^[6-9]\d{9}$/.test(clean);
}

export function detectLanguage(text: string): 'hindi' | 'hinglish' | 'english' {
  if (/[\u0900-\u097F]/.test(text)) return 'hindi';
  const lower = text.toLowerCase();
  const hinglishWords = ['aap', 'hai', 'nahi', 'kya', 'acha', 'bahut', 'thoda', 'kitna', 'kaisa', 'hume', 'chahiye', 'samajh', 'bilkul', 'zaroor', 'namaste', 'dhanyavaad'];
  if (hinglishWords.some(w => lower.includes(w))) return 'hinglish';
  return 'english';
}

export function mapLangToVoiceLang(lang: string): string {
  const map: Record<string, string> = {
    hindi: 'hi-IN',
    hinglish: 'hi-IN',
    english: 'en-IN',
  };
  return map[lang] || 'hi-IN';
}

export function cleanAIResponse(text: string): string {
  // Remove JSON blocks for display
  return text.replace(/```json[\s\S]*?```/g, '').replace(/```[\s\S]*?```/g, '').replace(/\{.*"customerName".*\}/g, '').trim();
}
