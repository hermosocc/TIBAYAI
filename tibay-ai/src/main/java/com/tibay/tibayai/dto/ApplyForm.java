package com.tibay.tibayai.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApplyForm {
	@Size(max = 1200)
	private String coverNote;
}

