package com.tibay.tibayai.config;

import java.nio.file.Path;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
	private final TibayProperties properties;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path uploadPath = Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
		registry.addResourceHandler("/uploads/**").addResourceLocations(uploadPath.toUri().toString());
	}
}

