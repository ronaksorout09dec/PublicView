import express from 'express';
import cors from 'cors';
import axios from 'axios';

const app = express();
const PORT = process.env.PORT || 8080;
const OLLAMA_BASE = process.env.OLLAMA_BASE_URL || 'http://localhost:11434';
const OLLAMA_MODEL = process.env.OLLAMA_MODEL || 'qwen3:latest';

app.use(cors({ origin: true, credentials: true }));
app.use(express.json({ limit: '10mb' }));

// Global error handler for invalid JSON, timeout, etc.
app.use((err, req, res, next) => {
  if (err instanceof SyntaxError && err.status === 400 && 'body' in err) {
    console.warn('[Error] Invalid JSON:', err.message);
    return res.status(400).json({ success: false, message: 'Invalid JSON payload', error: err.message, timestamp: new Date().toISOString() });
  }
  if (err.type === 'entity.too.large') {
    return res.status(413).json({ success: false, message: 'Payload too large', error: err.message });
  }
  next(err);
});

// Input sanitization helper (mirrors Java Sanitizer + InputSanitizationFilter)
function sanitize(input) {
  if (input == null) return input;
  return input.toString().replace(/<script.*?>.*?<\/script>/gi, '').replace(/javascript:/gi, '').trim();
}
function sanitizePhone(phone) {
  if (phone == null) return phone;
  return phone.toString().replace(/\D/g, '').trim();
}

// In-memory store (mirrors PostgreSQL Leads table)
let leads = [];
let leadIdCounter = 1;

// Logging middleware
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// Project info - cached
const PROJECT_INFO = `
PROJECT: Sky Heights Residency
LOCATION: Sector 150, Noida (on Expressway, near Metro)
CONFIGURATIONS:
- 2 BHK: ₹85 Lakhs
- 3 BHK: ₹1.2 Crore
- 4 BHK: ₹1.6 Crore
AMENITIES: Swimming Pool, Gym, Club House, Kids Area, Power Backup, 24x7 Security
POSSESSION: December 2028
NEARBY: Expressway, Metro Station, Hospitals, Schools, Mall
`;

const SYSTEM_PROMPT = `
You are Priya, a professional real estate sales executive for Sky Heights Residency, Sector 150 Noida.

PERSONALITY:
- Warm, professional, natural, friendly - like a real human sales executive, NOT an IVR
- Speak naturally, concise, under 3 sentences unless customer asks for details
- Switch automatically between Hindi, Hinglish, and English based on customer's language
- Ask ONLY ONE question at a time and wait for response
- Remember previous context perfectly, never repeat same question
- Handle interruptions naturally
- Never hallucinate - if information not available, say "I'll confirm this with our sales team"

${PROJECT_INFO}

CONVERSATION FLOW (MANDATORY):
1. Greeting + Introduce Company + Agent
2. Ask purpose (Buying vs Investment)
3. Preferred Location
4. Property Type
5. Configuration (2/3/4 BHK)
6. Budget (85L/1.2Cr/1.6Cr)
7. Purpose
8. Timeline
9. Handle Questions
10. Collect Contact: Name, Phone
11. Generate Summary + Thank Customer

LANGUAGE RULES:
- Detect customer language, respond in same language (Hindi/Hinglish/English)
- Hinglish example: "Bahut accha, aapko 2 BHK chahiye? Aapka budget kya hai?"
- If Hindi in Devanagari or Hindi words in Roman -> Hindi/Hinglish
- If English -> English
- Be natural, colloquial

COLLECTION REQUIREMENTS:
- Name, Phone (10 digits 6-9), Location, Budget, Configuration, Property Type, Purpose, Timeline
- Keep replies under 3 sentences unless asked for details
- For price: 2BHK 85L, 3BHK 1.2Cr, 4BHK 1.6Cr
- Possession: December 2028
- Never invent details

Keep inference natural, concise, human-like. Never sound robotic.
`;

// In-memory sessions
const sessions = new Map();

function getOrCreateSession(sessionId) {
  if (sessionId && sessions.has(sessionId)) return sessions.get(sessionId);
  const id = sessionId || `sess_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  const session = { id, messages: [], systemPrompt: SYSTEM_PROMPT, createdAt: Date.now() };
  sessions.set(id, session);
  return session;
}

async function callOllama(session, temperature = 0.7) {
  const url = `${OLLAMA_BASE}/api/chat`;
  console.log(`[Ollama] Calling ${url} model ${OLLAMA_MODEL} with ${session.messages.length} messages`);
  const messages = [
    { role: 'system', content: session.systemPrompt },
    ...session.messages.map(m => ({ role: m.role, content: m.content }))
  ];
  try {
    const res = await axios.post(url, {
      model: OLLAMA_MODEL,
      messages,
      stream: false,
      options: { temperature, num_predict: 500 }
    }, { timeout: 60000 });
    const content = res.data?.message?.content || res.data?.response;
    if (!content) throw new Error('Empty Ollama response');
    console.log(`[Ollama] Response: ${content.slice(0, 120)}...`);
    return content.trim();
  } catch (e) {
    console.error(`[Ollama] Failed: ${e.message}`);
    if (e.code === 'ECONNREFUSED' || e.message.includes('ECONNREFUSED')) {
      throw new Error(`Ollama unavailable at ${OLLAMA_BASE}. Ensure Ollama is running with ${OLLAMA_MODEL}`);
    }
    throw e;
  }
}

function fallbackResponse(userMessage, session) {
  const lower = userMessage.toLowerCase();
  const size = session.messages.length;
  console.log(`[Fallback] Using rule-based response for: "${userMessage}" size ${size}`);

  // Hallucination guard: unknown project details -> "I'll confirm with sales team"
  const unknownPatterns = [
    '1 bhk', '1bhk', '5 bhk', '5bhk', 'villa', 'plot', 'shop', 'office',
    '50 lakhs', '50lakh', '60 lakhs', '2 crore', '3 crore',
    'gurgaon', 'mumbai', 'bangalore', 'pune', 'delhi', 'faridabad',
    'possession 2025', 'possession 2026', 'ready to move'
  ];
  for (const pat of unknownPatterns) {
    if (lower.includes(pat)) {
      // Check if it's truly unknown (not in PROJECT_INFO)
      if (pat.includes('1 bhk') || pat.includes('50 lakhs') || pat.includes('villa') || pat.includes('plot') || pat.includes('gurgaon') || pat.includes('mumbai')) {
        return "I'll confirm this with our sales team. Hamare project me 2 BHK ₹85 Lakhs, 3 BHK ₹1.2 Crore, aur 4 BHK ₹1.6 Crore available hai Sector 150 Noida me. Aapko inme se kaunsa pasand hai?";
      }
    }
  }

  if (size <= 2) {
    return "Namaste! Main Priya bol rahi hu Sky Heights Residency se, Sector 150 Noida me. Hamara project Expressway aur Metro ke paas hai. Aap property buying ke liye dekh rahe hain ya investment ke liye?";
  }
  if (lower.includes("price") || lower.includes("budget") || lower.includes("kitna") || lower.includes("daam") || lower.includes("rate") || lower.includes("cost")) {
    return "Hamare yahan 2 BHK ₹85 Lakhs, 3 BHK ₹1.2 Crore, aur 4 BHK ₹1.6 Crore me available hai. Aapka budget kya hai aur aapko kaunsi configuration pasand hai?";
  }
  if (lower.includes("location") || lower.includes("kahan") || lower.includes("sector") || lower.includes("noida")) {
    return "Hamara project Sector 150 Noida me hai, Expressway aur Metro ke paas. Hospitals, Schools, Mall sab nearby hain. Aapko Noida me hi chahiye ya koi aur location?";
  }
  if (lower.includes("amenities") || lower.includes("facilities") || lower.includes("suvidha") || lower.includes("pool") || lower.includes("gym")) {
    return "Hamare project me Swimming Pool, Gym, Club House, Kids Area, Power Backup aur 24x7 Security hai. Possession December 2028 me milega. Aur kuch janna chahenge?";
  }
  if (lower.includes("possession") || lower.includes("kab") || lower.includes("when")) {
    return "Possession December 2028 me milega. Aapko kab tak chahiye - immediate ya 6 months me?";
  }
  if (lower.match(/\d{10}/) || lower.includes("phone") || lower.includes("number") || lower.includes("contact")) {
    return "Dhanyavaad! Aapka number note kar liya hai. Kya aap apna naam bhi bata sakte hain taki hamari team aapko sahi se contact kar sake?";
  }
  if (lower.includes("investment") || lower.includes("invest")) {
    return "Investment ke liye bahut accha choice hai! Sector 150 me appreciation bahut accha hai. Aapka budget kya hai aur aap kitne time ke liye investment dekh rahe hain?";
  }
  if (lower.includes("2 bhk") || lower.includes("3 bhk") || lower.includes("4 bhk")) {
    return "Bahut accha choice hai! Aapka budget kya hai aur aapko ye ghar khud rehne ke liye chahiye ya investment ke liye?";
  }
  // Generic - ask next in flow
  const lastAssistant = session.messages.filter(m => m.role === 'assistant').pop()?.content?.toLowerCase() || '';
  if (!lastAssistant.includes('budget')) {
    return "Samajh gayi. Aapka budget kya hai? Hamare paas 85 Lakhs se 1.6 Crore tak options hain.";
  }
  if (!lastAssistant.includes('timeline') && !lastAssistant.includes('kab')) {
    return "Aur aapko possession kab tak chahiye? Immediate, 3 months, 6 months ya 1 year me?";
  }
  if (!lastAssistant.includes('naam') && !lastAssistant.includes('name')) {
    return "Bahut badhiya. Toh main aapka naam aur phone number note kar lu taaki hamari team aapko detailed brochure aur site visit ke liye contact kar sake?";
  }
  return "Bilkul, aapki saari jankari note kar li hai. Kya aap site visit karna chahenge ya koi aur sawal hai?";
}

// Health
app.get('/api/health', async (req, res) => {
  let ollamaStatus = 'DOWN';
  let ollamaModel = OLLAMA_MODEL;
  try {
    const r = await axios.get(`${OLLAMA_BASE}/api/tags`, { timeout: 3000 });
    if (r.status === 200) ollamaStatus = 'UP';
  } catch (e) {
    console.log('[Health] Ollama down:', e.message);
  }
  res.json({
    success: true,
    message: 'Service is healthy',
    data: {
      status: 'UP',
      timestamp: new Date().toISOString(),
      service: 'Sky Heights Residency AI Voice Agent',
      version: '1.0.0',
      ollama: { status: ollamaStatus, model: ollamaModel, baseUrl: OLLAMA_BASE },
      database: { status: 'UP', type: 'PostgreSQL/H2 (in-memory mock)' },
      voice: { stt: 'Whisper (Faster-Whisper)', tts: 'Kokoro TTS (fallback Piper)', status: 'UP' }
    },
    timestamp: new Date().toISOString()
  });
});

// Leads CRUD - with sanitization (mirrors Java Sanitizer)
app.post('/api/leads', (req, res) => {
  let { customerName, phone, location, propertyType, configuration, budget, purpose, timeline, conversationSummary } = req.body;
  // Sanitize inputs
  if (customerName) customerName = sanitize(customerName);
  if (location) location = sanitize(location);
  if (propertyType) propertyType = sanitize(propertyType);
  if (configuration) configuration = sanitize(configuration);
  if (budget) budget = sanitize(budget);
  if (purpose) purpose = sanitize(purpose);
  if (timeline) timeline = sanitize(timeline);
  if (conversationSummary) conversationSummary = sanitize(conversationSummary);
  if (phone) phone = sanitizePhone(phone);
  console.log('[Leads] Create (sanitized):', { customerName, phone });
  if (!customerName || !customerName.trim()) {
    return res.status(400).json({ success: false, message: 'Validation failed', error: 'customerName is required', data: { customerName: 'Customer name is required' } });
  }
  if (!phone || !/^[6-9]\d{9}$/.test(phone.replace(/\D/g, ''))) {
    return res.status(400).json({ success: false, message: 'Validation failed', error: 'Invalid phone', data: { phone: 'Invalid Indian phone number - must be 10 digits starting with 6-9' } });
  }
  const cleanPhone = phone.replace(/\D/g, '');
  const lead = {
    id: leadIdCounter++,
    customerName: customerName.trim(),
    phone: cleanPhone,
    location: location || 'Sector 150 Noida',
    propertyType: propertyType || 'Apartment',
    configuration: configuration || null,
    budget: budget || null,
    purpose: purpose || null,
    timeline: timeline || null,
    conversationSummary: conversationSummary || '',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };
  leads.push(lead);
  console.log('[Leads] Created ID', lead.id);
  res.status(201).json({ success: true, message: 'Lead created successfully', data: lead, timestamp: new Date().toISOString() });
});

app.get('/api/leads', (req, res) => {
  console.log(`[Leads] Fetch all: ${leads.length}`);
  res.json({ success: true, message: 'Leads fetched successfully', data: leads, timestamp: new Date().toISOString() });
});

app.get('/api/leads/:id', (req, res) => {
  const id = parseInt(req.params.id);
  const lead = leads.find(l => l.id === id);
  if (!lead) return res.status(404).json({ success: false, message: 'Lead not found', error: 'NOT_FOUND' });
  res.json({ success: true, message: 'Lead fetched successfully', data: lead, timestamp: new Date().toISOString() });
});

app.delete('/api/leads/:id', (req, res) => {
  const id = parseInt(req.params.id);
  const idx = leads.findIndex(l => l.id === id);
  if (idx === -1) return res.status(404).json({ success: false, message: 'Lead not found', error: 'NOT_FOUND' });
  leads.splice(idx, 1);
  console.log(`[Leads] Deleted ${id}`);
  res.json({ success: true, message: 'Lead deleted successfully', data: null, timestamp: new Date().toISOString() });
});

// Voice Chat - Main AI endpoint (Ollama + Qwen3 with fallback)
app.post('/api/voice/chat', async (req, res) => {
  const { message, sessionId, language, history } = req.body;
  console.log(`[Voice Chat] session=${sessionId} lang=${language} msg="${message?.slice(0, 80)}"`);
  if (!message || !message.trim()) {
    return res.status(400).json({ success: false, message: 'Message is required' });
  }
  const session = getOrCreateSession(sessionId);
  // If history provided and session empty, populate
  if (history && Array.isArray(history) && session.messages.length === 0) {
    for (const m of history) {
      if (m.role === 'user' || m.role === 'assistant') {
        session.messages.push({ role: m.role, content: m.content });
      }
    }
  }
  // Add user message
  session.messages.push({ role: 'user', content: message });

  let aiResponse;
  try {
    aiResponse = await callOllama(session);
  } catch (e) {
    console.warn(`[Voice Chat] Ollama failed, using fallback: ${e.message}`);
    aiResponse = fallbackResponse(message, session);
  }

  session.messages.push({ role: 'assistant', content: aiResponse });

  // Simple language detection for response
  let detectedLanguage = language || 'hinglish';
  if (/[\u0900-\u097F]/.test(message)) detectedLanguage = 'hindi';
  else if (message.toLowerCase().match(/\b(aap|hai|nahi|kya|acha|bahut|thoda|kitna)\b/)) detectedLanguage = 'hinglish';

  const isLeadReady = aiResponse.includes('customerName') || aiResponse.includes('"phone"');
  res.json({
    success: true,
    message: 'Chat successful',
    data: {
      response: aiResponse,
      sessionId: session.id,
      detectedLanguage,
      leadReady: isLeadReady
    },
    timestamp: new Date().toISOString()
  });
});

// TTS / STT dummy endpoints (satisfy architecture)
app.post('/api/voice/tts', (req, res) => {
  const { text } = req.body;
  console.log(`[TTS] Text: ${text?.slice(0, 60)}`);
  res.json({ success: true, message: 'TTS info', data: { text: text || '', provider: 'kokoro', fallback: 'piper', note: 'TTS handled client-side via Web Speech API; for local Kokoro/Piper configure voice.tts.provider' } });
});

app.post('/api/voice/stt', (req, res) => {
  res.json({ success: true, message: 'STT info', data: { provider: 'faster-whisper', alternative: 'whisper.cpp', note: 'STT handled client-side via Web Speech API; for local Whisper send audio/wav' } });
});

// Call Summary - Generates structured JSON and saves lead
app.post('/api/call-summary', async (req, res) => {
  const { conversationHistory, customerName, phone } = req.body;
  console.log(`[Call Summary] history=${conversationHistory?.length} name=${customerName} phone=${phone}`);
  if (!conversationHistory || !Array.isArray(conversationHistory) || conversationHistory.length === 0) {
    return res.status(400).json({ success: false, message: 'Conversation history is required' });
  }

  // Build prompt for Ollama to extract structured data
  const summaryPrompt = `
You are an expert real estate CRM assistant. Generate a structured JSON summary from this conversation for Sky Heights Residency.
Conversation:
${conversationHistory.map(m => `${m.role}: ${m.content}`).join('\n')}

Extract fields as JSON. Use null if not mentioned:
{
  "customerName": "string or null",
  "phone": "10-digit Indian phone or null",
  "location": "string or null",
  "propertyType": "Apartment/Villa/etc or null",
  "configuration": "2 BHK/3 BHK/4 BHK or null",
  "budget": "string like '85 Lakhs' or null",
  "purpose": "Buying/Investment/Self-use/Rental or null",
  "timeline": "Immediate/3 months/6 months/1 year or null",
  "conversationSummary": "2-3 sentence summary"
}
Return ONLY valid JSON, no markdown.
`;

  let summaryJson;
  let parsed;
  try {
    // Try Ollama
    const tempSession = { systemPrompt: summaryPrompt, messages: conversationHistory };
    const ollamaRes = await callOllama(tempSession, 0.3);
    // Extract JSON
    const start = ollamaRes.indexOf('{');
    const end = ollamaRes.lastIndexOf('}');
    if (start !== -1 && end !== -1) {
      summaryJson = ollamaRes.slice(start, end + 1);
      parsed = JSON.parse(summaryJson);
    } else {
      throw new Error('No JSON in Ollama response');
    }
    console.log('[Call Summary] Ollama parsed:', parsed);
  } catch (e) {
    console.warn('[Call Summary] Ollama failed, using heuristic fallback:', e.message);
    // Heuristic fallback: parse conversation
    const allText = conversationHistory.map(m => m.content).join(' ').toLowerCase();
    const phoneMatch = allText.match(/[6-9]\d{9}/);
    const nameMatch = conversationHistory.find(m => m.role === 'user')?.content?.split(' ').slice(0, 2).join(' ') || customerName || 'Unknown';
    let config = null;
    if (allText.includes('2 bhk')) config = '2 BHK';
    else if (allText.includes('3 bhk')) config = '3 BHK';
    else if (allText.includes('4 bhk')) config = '4 BHK';
    let budget = null;
    if (allText.includes('85')) budget = '₹85 Lakhs';
    else if (allText.includes('1.2')) budget = '₹1.2 Crore';
    else if (allText.includes('1.6')) budget = '₹1.6 Crore';
    let purpose = null;
    if (allText.includes('investment')) purpose = 'Investment';
    else if (allText.includes('buy') || allText.includes('living') || allText.includes('rehne')) purpose = 'Buying';
    parsed = {
      customerName: customerName || nameMatch || 'Unknown',
      phone: phone || (phoneMatch ? phoneMatch[0] : null),
      location: allText.includes('noida') || allText.includes('sector 150') ? 'Sector 150 Noida' : 'Sector 150 Noida',
      propertyType: 'Apartment',
      configuration: config,
      budget,
      purpose,
      timeline: allText.includes('immediate') ? 'Immediate' : allText.includes('3 month') ? '3 months' : allText.includes('6 month') ? '6 months' : null,
      conversationSummary: conversationHistory.slice(-4).map(m => `${m.role}: ${m.content.slice(0, 60)}`).join('; ').slice(0, 300)
    };
    summaryJson = JSON.stringify(parsed);
  }

  // Validate and save lead if possible
  const leadData = {
    customerName: parsed.customerName && parsed.customerName !== 'null' ? parsed.customerName : (customerName || 'Unknown'),
    phone: parsed.phone && parsed.phone !== 'null' ? parsed.phone.toString().replace(/\D/g, '') : (phone ? phone.toString().replace(/\D/g, '') : ''),
    location: parsed.location || 'Sector 150 Noida',
    propertyType: parsed.propertyType || 'Apartment',
    configuration: parsed.configuration || null,
    budget: parsed.budget || null,
    purpose: parsed.purpose || null,
    timeline: parsed.timeline || null,
    conversationSummary: parsed.conversationSummary || conversationHistory.map(m => m.content).join(' ').slice(0, 500)
  };

  // Check if we have valid data to save
  let savedLead = null;
  let message = 'Summary generated';
  if (leadData.customerName && leadData.phone && /^[6-9]\d{9}$/.test(leadData.phone)) {
    const lead = {
      id: leadIdCounter++,
      ...leadData,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };
    leads.push(lead);
    savedLead = lead;
    message = 'Summary generated and lead saved';
    console.log(`[Call Summary] Saved lead ${lead.id}`);
  } else {
    message = 'Summary generated (insufficient data to save lead - need valid Name and Phone)';
    console.log('[Call Summary] Not saved - invalid data:', leadData);
  }

  res.json({
    success: true,
    message,
    data: {
      summary: leadData.conversationSummary,
      lead: savedLead,
      structuredJson: summaryJson
    },
    timestamp: new Date().toISOString()
  });
});

// Catch all
app.use((req, res) => {
  res.status(404).json({ success: false, message: `Not found: ${req.method} ${req.path}` });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`
╔════════════════════════════════════════════════╗
║  Sky Heights Residency - AI Voice Agent      ║
║  Mock Backend (Node) running on port ${PORT}     ║
║  Ollama: ${OLLAMA_BASE} model ${OLLAMA_MODEL} ║
║  Project: Sector 150 Noida - Dec 2028        ║
╚════════════════════════════════════════════════╝
  `);
  console.log(`Health: http://localhost:${PORT}/api/health`);
  console.log(`Leads:  http://localhost:${PORT}/api/leads`);
});
