package com.tibay.tibayai.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortfolioForm {
	@Size(max = 1200)
	private String rawText;

	@Size(max = 6000)
	private String cvResults;
}
