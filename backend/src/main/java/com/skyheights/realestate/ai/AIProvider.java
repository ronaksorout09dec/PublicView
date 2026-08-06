package com.skyheights.realestate.ai;

import java.util.List;

/**
 * AI Provider abstraction.
 * Only AIService may call implementations of this interface.
 * Controllers must never communicate directly with Ollama.
 */
public interface AIProvider {

    /**
     * Generate a chat completion response.
     *
     * @param systemPrompt system instructions
     * @param messages conversation history
     * @return AI generated text
     */
    String generateResponse(String systemPrompt, List<ConversationMessage> messages);

    /**
     * Generate a chat completion with full conversation session.
     *
     * @param session conversation session containing history
     * @return AI generated text
     */
    String generateResponse(ConversationSession session);

    /**
     * Check if provider is available
     * @return true if Ollama is reachable
     */
    boolean isAvailable();

    /**
     * Get model information
     * @return model name
     */
    String getModelName();
}
