package com.tibay.tibayai.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobPostForm {
	@NotBlank
	private String title;

	@NotBlank
	private String description;

	@NotBlank
	private String specialization;

	@NotNull
	@Min(0)
	private Integer budgetPhp;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate deadline;

	private String barangay;

	private String city;
}

