package com.skyheights.realestate.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessage {
    private String role; // system, user, assistant
    private String content;
    private long timestamp;

    public static ConversationMessage user(String content) {
        return ConversationMessage.builder().role("user").content(content).timestamp(System.currentTimeMillis()).build();
    }

    public static ConversationMessage assistant(String content) {
        return ConversationMessage.builder().role("assistant").content(content).timestamp(System.currentTimeMillis()).build();
    }

    public static ConversationMessage system(String content) {
        return ConversationMessage.builder().role("system").content(content).timestamp(System.currentTimeMillis()).build();
    }
}
