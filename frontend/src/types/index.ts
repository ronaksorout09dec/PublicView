export interface Lead {
  id?: number;
  customerName: string;
  phone: string;
  location?: string;
  propertyType?: string;
  configuration?: string;
  budget?: string;
  purpose?: string;
  timeline?: string;
  conversationSummary?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ConversationMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp?: string;
}

export interface ChatRequest {
  message: string;
  sessionId?: string;
  language?: string;
  history?: ConversationMessage[];
}

export interface ChatResponse {
  response: string;
  sessionId: string;
  detectedLanguage?: string;
  leadReady?: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  error?: string;
  timestamp?: string;
}

export interface HealthStatus {
  status: string;
  timestamp: string;
  service: string;
  version: string;
  ollama: {
    status: string;
    model: string;
    baseUrl: string;
  };
  database: {
    status: string;
    type: string;
  };
  voice: {
    stt: string;
    tts: string;
    status: string;
  };
}

export type Language = 'hindi' | 'hinglish' | 'english' | 'auto';
export type CallStatus = 'idle' | 'connecting' | 'active' | 'ended' | 'error';
