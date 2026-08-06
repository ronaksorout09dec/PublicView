import axios from 'axios';
import type { Lead, ConversationMessage, ApiResponse, HealthStatus, ChatRequest, ChatResponse } from '../types';

const API_BASE = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE,
  timeout: 65000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Log requests
api.interceptors.request.use((config) => {
  console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('[API Error]', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export const leadApi = {
  create: async (lead: Partial<Lead>) => {
    const res = await api.post<ApiResponse<Lead>>('/leads', lead);
    return res.data;
  },
  getAll: async () => {
    const res = await api.get<ApiResponse<Lead[]>>('/leads');
    return res.data;
  },
  getById: async (id: number) => {
    const res = await api.get<ApiResponse<Lead>>(`/leads/${id}`);
    return res.data;
  },
  delete: async (id: number) => {
    const res = await api.delete<ApiResponse<void>>(`/leads/${id}`);
    return res.data;
  },
};

export const voiceApi = {
  chat: async (payload: ChatRequest) => {
    const res = await api.post<ApiResponse<ChatResponse>>('/voice/chat', payload);
    return res.data;
  },
  tts: async (text: string) => {
    const res = await api.post<ApiResponse<{ text: string }>>('/voice/tts', { text });
    return res.data;
  },
  stt: async () => {
    const res = await api.post<ApiResponse<{ provider: string }>>('/voice/stt', {});
    return res.data;
  },
};

export const callSummaryApi = {
  create: async (history: ConversationMessage[], customerName?: string, phone?: string) => {
    const res = await api.post<ApiResponse<{ summary: string; lead: Lead; structuredJson: string }>>('/call-summary', {
      conversationHistory: history.map(m => ({
        role: m.role,
        content: m.content,
        timestamp: m.timestamp || new Date().toISOString()
      })),
      customerName,
      phone
    });
    return res.data;
  }
};

export const healthApi = {
  check: async () => {
    const res = await api.get<ApiResponse<HealthStatus>>('/health');
    return res.data;
  }
};

export default api;
