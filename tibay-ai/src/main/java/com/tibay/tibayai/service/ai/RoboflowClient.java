package com.tibay.tibayai.service.ai;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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
public class RoboflowClient {
	public record Detection(String label, double confidence, int x, int y, int width, int height) {
	}

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final TibayProperties properties;

	public boolean enabled() {
		return facesEnabled() || ppeEnabled();
	}

	public boolean facesEnabled() {
		return StringUtils.hasText(properties.getRoboflowApiKey()) && StringUtils.hasText(properties.getRoboflowFaceModelUrl());
	}

	public boolean ppeEnabled() {
		return StringUtils.hasText(properties.getRoboflowApiKey()) && StringUtils.hasText(properties.getRoboflowPpeModelUrl());
	}

	public List<Detection> detectFaces(File imageFile) {
		return detect(imageFile, properties.getRoboflowFaceModelUrl());
	}

	public List<Detection> detectPpe(File imageFile) {
		return detect(imageFile, properties.getRoboflowPpeModelUrl());
	}

	private List<Detection> detect(File imageFile, String modelUrl) {
		if (!StringUtils.hasText(properties.getRoboflowApiKey()) || !StringUtils.hasText(modelUrl)) {
			return List.of();
		}
		try {
			String url = modelUrl;
			if (!url.contains("?")) {
				url = url + "?api_key=" + properties.getRoboflowApiKey();
			} else {
				url = url + "&api_key=" + properties.getRoboflowApiKey();
			}

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new FileSystemResource(imageFile));

			String raw = restClient.post()
					.uri(url)
					.contentType(MediaType.MULTIPART_FORM_DATA)
					.body(body)
					.retrieve()
					.body(String.class);

			return parseDetections(raw);
		} catch (Exception e) {
			return List.of();
		}
	}

	private List<Detection> parseDetections(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		try {
			JsonNode node = objectMapper.readTree(raw);
			JsonNode preds = node.path("predictions");
			if (!preds.isArray()) {
				return List.of();
			}
			List<Detection> out = new ArrayList<>();
			for (JsonNode p : preds) {
				String label = Optional.ofNullable(p.path("class").asText(null)).orElse("object");
				double conf = p.path("confidence").asDouble(0.0);
				int x = (int) Math.round(p.path("x").asDouble(0.0));
				int y = (int) Math.round(p.path("y").asDouble(0.0));
				int w = (int) Math.round(p.path("width").asDouble(0.0));
				int h = (int) Math.round(p.path("height").asDouble(0.0));
				out.add(new Detection(label, conf, x, y, w, h));
			}
			return out;
		} catch (Exception e) {
			return List.of();
		}
	}
}
