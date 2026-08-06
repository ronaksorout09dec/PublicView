package com.skyheights.realestate.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private String sessionId;

    private String language; // hindi, hinglish, english, auto

    private List<ChatMessageDto> history;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatMessageDto {
        private String role;
        private String content;
    }
}
