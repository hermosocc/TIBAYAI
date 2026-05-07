package com.tibay.tibayai.service.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibay.tibayai.config.TibayProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OpenRouterClient {
	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final TibayProperties properties;

	public boolean enabled() {
		return StringUtils.hasText(properties.getOpenrouterApiKey());
	}

	public Optional<String> chat(String systemPrompt, String userPrompt) {
		if (!enabled()) {
			return Optional.empty();
		}
		try {
			Map<String, Object> payload = Map.of(
					"model", properties.getOpenrouterModel(),
					"messages", List.of(
							Map.of("role", "system", "content", systemPrompt),
							Map.of("role", "user", "content", userPrompt)));

			String raw = restClient.post()
					.uri("https://openrouter.ai/api/v1/chat/completions")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getOpenrouterApiKey())
					.header("HTTP-Referer", "http://localhost")
					.header("X-Title", "TIBAY AI MVP")
					.contentType(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(String.class);

			JsonNode node = objectMapper.readTree(raw);
			JsonNode choices = node.path("choices");
			if (choices.isArray() && !choices.isEmpty()) {
				String content = choices.get(0).path("message").path("content").asText(null);
				return Optional.ofNullable(content);
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}

