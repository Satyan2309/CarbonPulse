package com.ecotrack.service.impl;

import com.ecotrack.dto.RecommendationResponse;
import com.ecotrack.entity.CarbonEmission;
import com.ecotrack.entity.User;
import com.ecotrack.exception.ResourceNotFoundException;
import com.ecotrack.repository.CarbonEmissionRepository;
import com.ecotrack.repository.UserRepository;
import com.ecotrack.service.AIService;
import com.ecotrack.utils.PromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private static final String OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini";

    private final CarbonEmissionRepository carbonEmissionRepository;
    private final UserRepository userRepository;
    private final PromptBuilder promptBuilder;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse recommend(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CarbonEmission> emissions = carbonEmissionRepository.findByUserId(user.getId());
        String prompt = promptBuilder.buildPrompt(user, emissions);

        String responseBody = callOpenAi(prompt);
        List<String> recommendations = parseRecommendations(responseBody);

        return RecommendationResponse.builder()
                .recommendations(recommendations)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.ecotrack.dto.AIChatResponse chat(String email, String userMessage) {
        // Try OpenAI if key is configured and not the demo placeholder
        if (openAiApiKey != null && !openAiApiKey.isBlank() && !"demo".equalsIgnoreCase(openAiApiKey)) {
            try {
                String reply = callOpenAiChat(email, userMessage);
                return new com.ecotrack.dto.AIChatResponse(reply);
            } catch (Exception ex) {
                // Fall through to rule-based response
            }
        }
        // Rule-based fallback — works without any OpenAI key
        return new com.ecotrack.dto.AIChatResponse(generateRuleBasedReply(userMessage));
    }

    /**
     * Calls OpenAI with a carbon-context-aware system prompt for conversational chat.
     */
    private String callOpenAiChat(String email, String userMessage) {
        // Build a brief carbon context summary for the system prompt
        String carbonContext = buildCarbonContextSummary(email);

        String systemPrompt = "You are EcoAI, a friendly and concise sustainability advisor integrated into EcoTrack, "
                + "a carbon footprint tracking app. " + carbonContext
                + " Answer the user's question in 2-4 sentences, giving practical, actionable advice. "
                + "Do not use JSON. Respond in plain text only.";

        OpenAiChatRequest requestPayload = new OpenAiChatRequest(
                OPENAI_MODEL,
                List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userMessage)
                ),
                null   // null = text mode (no JSON response_format)
        );

        try {
            // Build request JSON manually without response_format for text mode
            String requestJson = buildChatRequestJson(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_ENDPOINT))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI chat failed: " + response.statusCode());
            }

            String content = extractAssistantContent(response.body());
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Empty reply from OpenAI");
            }
            return content;

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI chat interrupted", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("AI chat IO error", ex);
        }
    }

    /**
     * Builds a brief carbon data summary to inject into the AI system prompt,
     * giving the model user-specific context.
     */
    private String buildCarbonContextSummary(String email) {
        try {
            User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (user == null) return "";
            List<CarbonEmission> recent = carbonEmissionRepository.findByUserId(user.getId());
            if (recent.isEmpty()) {
                return "The user has not logged any carbon activities yet. ";
            }
            long count = recent.size();
            java.math.BigDecimal total = recent.stream()
                    .map(CarbonEmission::getTotalEmission)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            return String.format("The user has logged %d carbon activities with a total of %.2f kg CO\u2082 recorded. ", count, total);
        } catch (Exception ex) {
            return "";
        }
    }

    /**
     * Rule-based keyword matching fallback — zero external dependencies.
     * Provides useful answers even without an OpenAI API key.
     */
    private String generateRuleBasedReply(String userMessage) {
        String query = userMessage.toLowerCase();

        if (query.contains("electricity") || query.contains("home energy") || query.contains("power bill")) {
            return "To reduce home electricity consumption: switch to LED lighting, unplug standby devices, "
                    + "use a smart thermostat, and consider Energy Star-rated appliances. "
                    + "These changes can save up to 450 kg CO\u2082 per year.";
        }
        if (query.contains("transport") || query.contains("car") || query.contains("ev") || query.contains("drive") || query.contains("commute")) {
            return "Public transport and cycling are the lowest-carbon commute options. "
                    + "Electric vehicles (EVs) reduce lifecycle emissions by ~60% vs petrol engines. "
                    + "If driving is unavoidable, carpooling halves your per-trip footprint.";
        }
        if (query.contains("food") || query.contains("diet") || query.contains("eat") || query.contains("meat") || query.contains("vegan")) {
            return "A plant-rich diet is one of the highest-impact actions for reducing your carbon footprint. "
                    + "Reducing red meat by 50% can save up to 500 kg CO\u2082 annually. "
                    + "Try Meatless Mondays as a starting step.";
        }
        if (query.contains("water") || query.contains("shower") || query.contains("tap")) {
            return "Reducing hot water use lowers both water and energy emissions. "
                    + "Shorter showers (under 5 minutes), fixing leaky taps, and using a cold-wash laundry cycle "
                    + "can collectively reduce household carbon by around 100 kg CO\u2082 per year.";
        }
        if (query.contains("waste") || query.contains("recycle") || query.contains("plastic") || query.contains("rubbish")) {
            return "Waste reduction is most effective in this order: Refuse, Reduce, Reuse, Recycle. "
                    + "Composting food scraps cuts methane emissions from landfills significantly. "
                    + "Avoiding single-use plastics reduces both production and disposal emissions.";
        }
        if (query.contains("goal") || query.contains("target") || query.contains("reduce") || query.contains("improve")) {
            return "A practical starting goal is to reduce your monthly carbon footprint by 10% over 3 months. "
                    + "Log your activities daily in EcoTrack to identify your biggest emission source, "
                    + "then focus your efforts there first — that's where you'll see the greatest impact.";
        }
        if (query.contains("challenge") || query.contains("badge") || query.contains("point") || query.contains("reward")) {
            return "EcoTrack challenges help you build sustainable habits through gamification. "
                    + "Completing a challenge earns badge XP. "
                    + "Start with the 'Plastic-Free Week' or 'Cycle to Work' challenges — they have quick, measurable impact.";
        }
        if (query.contains("air") || query.contains("flight") || query.contains("plane") || query.contains("travel")) {
            return "Flights are one of the most carbon-intensive activities per hour. "
                    + "A single long-haul return flight can produce 2–3 tonnes of CO\u2082. "
                    + "Where possible, take trains for journeys under 700 km — they produce up to 90% less CO\u2082.";
        }
        // Default
        return "Small, consistent changes in daily habits can reduce your overall footprint by 20-25% annually. "
                + "Start by tracking your biggest emission sources in EcoTrack — transport, food, and home energy "
                + "typically account for over 70% of a household's carbon footprint. "
                + "Focus your efforts there for the highest impact.";
    }



    private String callOpenAi(String prompt) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        OpenAiChatRequest requestPayload = new OpenAiChatRequest(
                OPENAI_MODEL,
                List.of(
                        new ChatMessage("system", "You are a sustainability assistant that returns only valid JSON."),
                        new ChatMessage("user", prompt)
                ),
                new ResponseFormat("json_object")
        );

        try {
            String requestJson = buildRequestJson(requestPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_ENDPOINT))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("OpenAI request failed with status code: " + response.statusCode());
            }

            String content = extractAssistantContent(response.body());
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("OpenAI returned an empty recommendation payload");
            }

            return content;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to generate AI recommendations", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate AI recommendations", ex);
        }
    }

    private List<String> parseRecommendations(String responseBody) {
        Pattern arrayPattern = Pattern.compile("\\\"recommendations\\\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);
        Matcher arrayMatcher = arrayPattern.matcher(responseBody);

        if (!arrayMatcher.find()) {
            throw new IllegalStateException("Unable to parse AI recommendations");
        }

        String arrayContent = arrayMatcher.group(1);
        Pattern itemPattern = Pattern.compile("\\\"((?:\\\\.|[^\\\"])*)\\\"");
        Matcher itemMatcher = itemPattern.matcher(arrayContent);

        List<String> recommendations = new java.util.ArrayList<>();
        while (itemMatcher.find()) {
            recommendations.add(unescapeJsonString(itemMatcher.group(1)));
        }

        if (recommendations.isEmpty()) {
            throw new IllegalStateException("OpenAI returned no recommendations");
        }

        return recommendations.stream().limit(5).toList();
    }

    /**
     * Builds request JSON without the response_format field, for plain text (chat) mode.
     * This prevents OpenAI from enforcing JSON-only output, allowing conversational replies.
     */
    private String buildChatRequestJson(OpenAiChatRequest requestPayload) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"model\":\"").append(escapeJson(requestPayload.model())).append("\",");
        builder.append("\"messages\":[");
        for (int index = 0; index < requestPayload.messages().size(); index++) {
            ChatMessage message = requestPayload.messages().get(index);
            builder.append('{')
                    .append("\"role\":\"").append(escapeJson(message.role())).append("\",")
                    .append("\"content\":\"").append(escapeJson(message.content())).append("\"")
                    .append('}');
            if (index < requestPayload.messages().size() - 1) {
                builder.append(',');
            }
        }
        builder.append("]");
        builder.append('}');
        return builder.toString();
    }

    private String buildRequestJson(OpenAiChatRequest requestPayload) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"model\":\"").append(escapeJson(requestPayload.model())).append("\",");
        builder.append("\"messages\":[");
        for (int index = 0; index < requestPayload.messages().size(); index++) {
            ChatMessage message = requestPayload.messages().get(index);
            builder.append('{')
                    .append("\"role\":\"").append(escapeJson(message.role())).append("\",")
                    .append("\"content\":\"").append(escapeJson(message.content())).append("\"")
                    .append('}');
            if (index < requestPayload.messages().size() - 1) {
                builder.append(',');
            }
        }
        builder.append("],");
        builder.append("\"response_format\":{\"type\":\"")
                .append(escapeJson(requestPayload.responseFormat().type()))
                .append("\"}");
        builder.append('}');
        return builder.toString();
    }

    private String extractAssistantContent(String responseBody) {
        Pattern contentPattern = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"", Pattern.DOTALL);
        Matcher contentMatcher = contentPattern.matcher(responseBody);

        if (!contentMatcher.find()) {
            throw new IllegalStateException("OpenAI returned no recommendations");
        }

        return unescapeJsonString(contentMatcher.group(1));
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJsonString(String value) {
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);

            if (escaping) {
                switch (current) {
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case '/' -> builder.append('/');
                    case 'b' -> builder.append('\b');
                    case 'f' -> builder.append('\f');
                    case 'n' -> builder.append('\n');
                    case 'r' -> builder.append('\r');
                    case 't' -> builder.append('\t');
                    default -> builder.append(current);
                }
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }

            builder.append(current);
        }

        return builder.toString();
    }

    private record OpenAiChatRequest(String model, List<ChatMessage> messages, ResponseFormat responseFormat) {
    }

    private record ChatMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }
}

