package com.tibay.tibayai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageForm {
	@NotBlank
	@Size(max = 2000)
	private String body;
}

