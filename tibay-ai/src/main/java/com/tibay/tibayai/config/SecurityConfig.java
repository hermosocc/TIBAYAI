package com.tibay.tibayai.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/login", "/register/**", "/css/**", "/js/**", "/uploads/**").permitAll()
				.requestMatchers("/worker/**").hasRole("WORKER")
				.requestMatchers("/client/**").hasRole("CLIENT")
				.anyRequest().authenticated());

		http.formLogin(form -> form.loginPage("/login").successHandler(successHandler()).permitAll());
		http.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/"));
		http.csrf(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	AuthenticationSuccessHandler successHandler() {
		return new AuthenticationSuccessHandler() {
			@Override
			public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
					org.springframework.security.core.Authentication authentication) throws IOException, ServletException {
				boolean isClient = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENT"));
				boolean isWorker = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_WORKER"));
				if (isClient) {
					response.sendRedirect("/client/dashboard");
					return;
				}
				if (isWorker) {
					response.sendRedirect("/worker/dashboard");
					return;
				}
				response.sendRedirect("/");
			}
		};
	}
}

