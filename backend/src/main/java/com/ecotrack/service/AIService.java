package com.ecotrack.service;

import com.ecotrack.dto.AIChatResponse;
import com.ecotrack.dto.RecommendationResponse;

public interface AIService {

    RecommendationResponse recommend(String authenticatedEmail);

    /**
     * Handles a conversational chat message from the user.
     * Uses OpenAI with the user's carbon context embedded in the system prompt.
     * Falls back to rule-based keyword matching if OpenAI is unavailable or unconfigured.
     *
     * @param email       the authenticated user's email
     * @param userMessage the message typed by the user
     * @return an {@link AIChatResponse} containing the assistant's reply
     */
    AIChatResponse chat(String email, String userMessage);
}
