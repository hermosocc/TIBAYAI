package com.tibay.tibayai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterWorkerForm {
	@NotBlank
	@Email
	private String email;

	@NotBlank
	@Size(min = 6, max = 50)
	private String password;

	@NotBlank
	private String fullName;

	@NotBlank
	@Size(min = 7, max = 20)
	@Pattern(regexp = "^[0-9+()\\-\\s]{7,20}$")
	private String phone;

	@NotBlank
	private String barangay;

	@NotBlank
	private String city;
}
