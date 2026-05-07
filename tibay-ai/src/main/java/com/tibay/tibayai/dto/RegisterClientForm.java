package com.tibay.tibayai.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterClientForm {
	@NotBlank
	@Email
	private String email;

	@NotBlank
	@Size(min = 6, max = 50)
	private String password;

	@NotBlank
	private String companyName;

	@NotBlank
	private String contactName;

	private String phone;

	private String barangay;

	private String city;
}

