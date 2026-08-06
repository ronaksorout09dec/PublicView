package com.skyheights.realestate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private String response;
    private String sessionId;
    private String detectedLanguage;
    private boolean leadReady;
    private LeadResponse extractedLead;
    private String audioBase64; // optional TTS audio
}
