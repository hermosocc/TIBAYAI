package com.tibay.tibayai.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortfolioForm {
	@Size(max = 1200)
	private String rawText;
}

