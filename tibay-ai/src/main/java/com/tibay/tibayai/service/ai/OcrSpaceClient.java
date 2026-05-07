package com.tibay.tibayai.service.ai;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.core.io.FileSystemResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibay.tibayai.config.TibayProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OcrSpaceClient {
	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final TibayProperties properties;

	public boolean enabled() {
		return StringUtils.hasText(properties.getOcrSpaceApiKey());
	}

	public Optional<String> extractText(java.io.File imageFile) {
		if (!enabled()) {
			return Optional.empty();
		}
		try {
			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("apikey", properties.getOcrSpaceApiKey());
			body.add("language", "eng");
			body.add("isOverlayRequired", "false");
			body.add("file", new FileSystemResource(imageFile));

			String raw = restClient.post()
					.uri("https://api.ocr.space/parse/image")
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body)
					.retrieve()
					.body(String.class);

			if (raw == null) {
				return Optional.empty();
			}
			JsonNode node = objectMapper.readTree(raw.getBytes(StandardCharsets.UTF_8));
			JsonNode parsedResults = node.path("ParsedResults");
			if (parsedResults.isArray() && !parsedResults.isEmpty()) {
				String text = parsedResults.get(0).path("ParsedText").asText(null);
				return Optional.ofNullable(text);
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}

