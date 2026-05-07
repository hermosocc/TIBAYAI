package com.tibay.tibayai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tibay.tibayai.config.TibayProperties;

@SpringBootApplication
@EnableConfigurationProperties(TibayProperties.class)
public class TibayAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TibayAiApplication.class, args);
	}

}
