package com.ziprun.reassignment.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * OpenAI-compatible /v1/chat/completions gateway implementation.
 * Credential and cookie headers are applied securely and never logged.
 */
@Component
public class OpenAiLlmGateway implements LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmGateway.class);

    private final String baseUrl;
    private final String apiKey;
    private final String cookie;
    private final String model;
    private final String product;
    private final int timeoutSeconds;
    private final RestClient restClient;

    public OpenAiLlmGateway(
            @Value("${llm.base-url:}") String baseUrl,
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.cookie:}") String cookie,
            @Value("${llm.model:qwen-cursor}") String model,
            @Value("${llm.product:PCT}") String product,
            @Value("${llm.timeout-seconds:10}") int timeoutSeconds) {
        this.baseUrl = baseUrl != null ? baseUrl.trim() : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.cookie = cookie != null ? cookie.trim() : "";
        this.model = (model != null && !model.isBlank()) ? model.trim() : "qwen-cursor";
        this.product = (product != null && !product.isBlank()) ? product.trim() : "PCT";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 10;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(this.timeoutSeconds).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(this.timeoutSeconds).toMillis());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        log.info("Initialized OpenAiLlmGateway with model '{}' and timeout {}s (endpoint configured: {})",
                this.model, this.timeoutSeconds, !this.baseUrl.isBlank());
    }

    @Override
    public String generateCompletion(String prompt) {
        if (baseUrl.isBlank()) {
            throw new IllegalStateException("LLM base URL is not configured (set LLM_BASE_URL environment variable)");
        }

        String endpointUrl = buildEndpointUrl(baseUrl);
        log.info("Sending chat completion request to endpoint [{}] with model '{}'", endpointUrl, model);

        OpenAiChatRequest requestPayload = new OpenAiChatRequest(
                model,
                List.of(new OpenAiChatRequest.Message("user", prompt)),
                0.2
        );

        OpenAiChatResponse response = restClient.post()
                .uri(endpointUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> applyAuthHeaders(headers))
                .body(requestPayload)
                .retrieve()
                .body(OpenAiChatResponse.class);

        if (response == null) {
            throw new IllegalStateException("Received empty response body from LLM endpoint");
        }

        String content = response.getFirstChoiceContent();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Received null or blank message content from LLM response");
        }

        return content;
    }

    private void applyAuthHeaders(HttpHeaders headers) {
        if (!apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }
        if (!cookie.isBlank()) {
            headers.set(HttpHeaders.COOKIE, cookie);
        }
        if (!product.isBlank()) {
            headers.set("product", product);
            headers.set("x-litellm-tags", "product:" + product);
        }
    }

    private String buildEndpointUrl(String base) {
        String trimmed = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        if (trimmed.endsWith("/v1/chat/completions")) {
            return trimmed;
        } else if (trimmed.endsWith("/v1")) {
            return trimmed + "/chat/completions";
        } else {
            return trimmed + "/v1/chat/completions";
        }
    }
}
