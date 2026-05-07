package com.tibay.tibayai.service.ai;

import java.io.File;
import java.util.Optional;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibay.tibayai.config.TibayProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InsightFaceClient {
	public record MatchResult(boolean samePerson, double score) {
	}

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final TibayProperties properties;

	public boolean enabled() {
		return StringUtils.hasText(properties.getInsightfaceUrl());
	}

	public Optional<MatchResult> match(File idImage, File selfieImage) {
		if (!enabled()) {
			return Optional.empty();
		}
		try {
			String url = properties.getInsightfaceUrl();
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			url = url + "/face/match";

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("id_image", new FileSystemResource(idImage));
			body.add("selfie_image", new FileSystemResource(selfieImage));

			String raw = restClient.post()
					.uri(url)
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body)
					.retrieve()
					.body(String.class);

			return parse(raw);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private Optional<MatchResult> parse(String raw) {
		if (!StringUtils.hasText(raw)) {
			return Optional.empty();
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			if (!node.path("ok").asBoolean(false)) {
				return Optional.empty();
			}
			boolean same = node.path("same_person").asBoolean(false);
			double score = node.path("score").asDouble(0.0);
			return Optional.of(new MatchResult(same, score));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
