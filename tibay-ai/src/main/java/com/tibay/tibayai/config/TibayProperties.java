package com.tibay.tibayai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "tibay")
public class TibayProperties {
	private String uploadDir = "./uploads";
	private boolean seedDemo = true;

	private String openrouterApiKey;
	private String openrouterModel = "openai/gpt-4o-mini";

	private String ocrSpaceApiKey;

	private String roboflowApiKey;
	private String roboflowPpeModelUrl;
	private String roboflowFaceModelUrl;

	private String insightfaceUrl;
}
