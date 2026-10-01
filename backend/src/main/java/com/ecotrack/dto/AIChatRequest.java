package com.ecotrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the AI chat endpoint POST /api/ai/chat.
 */
public class AIChatRequest {

    @NotBlank(message = "Message cannot be blank")
    @Size(max = 1000, message = "Message must be at most 1000 characters")
    private String message;

    public AIChatRequest() {}

    public AIChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
