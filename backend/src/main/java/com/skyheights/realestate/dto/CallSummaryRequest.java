package com.skyheights.realestate.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallSummaryRequest {

    @NotEmpty(message = "Conversation history is required")
    private List<ConversationMessageDto> conversationHistory;

    private String customerName;
    private String phone;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversationMessageDto {
        private String role; // user, assistant, system
        private String content;
        private String timestamp;
    }
}
