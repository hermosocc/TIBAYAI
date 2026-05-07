package com.tibay.tibayai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.tibay.tibayai.dto.RegisterClientForm;
import com.tibay.tibayai.dto.RegisterWorkerForm;
import com.tibay.tibayai.repo.UserRepository;
import com.tibay.tibayai.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {
	private final UserService userService;
	private final UserRepository userRepository;

	@GetMapping("/worker")
	public String registerWorkerForm(Model model) {
		model.addAttribute("form", new RegisterWorkerForm());
		return "auth/register-worker";
	}

	@PostMapping("/worker")
	public String registerWorkerSubmit(@Valid @ModelAttribute("form") RegisterWorkerForm form, BindingResult br, Model model) {
		if (userRepository.findByEmail(form.getEmail()).isPresent()) {
			br.rejectValue("email", "email.exists", "Email already registered");
		}
		if (br.hasErrors()) {
			return "auth/register-worker";
		}
		userService.registerWorker(form.getEmail(), form.getPassword(), form.getFullName(), form.getPhone(), form.getBarangay(), form.getCity());
		return "redirect:/login?registered";
	}

	@GetMapping("/client")
	public String registerClientForm(Model model) {
		model.addAttribute("form", new RegisterClientForm());
		return "auth/register-client";
	}

	@PostMapping("/client")
	public String registerClientSubmit(@Valid @ModelAttribute("form") RegisterClientForm form, BindingResult br, Model model) {
		if (userRepository.findByEmail(form.getEmail()).isPresent()) {
			br.rejectValue("email", "email.exists", "Email already registered");
		}
		if (br.hasErrors()) {
			return "auth/register-client";
		}
		userService.registerClient(form.getEmail(), form.getPassword(), form.getCompanyName(), form.getContactName(), form.getPhone(),
				form.getBarangay(), form.getCity());
		return "redirect:/login?registered";
	}
}

