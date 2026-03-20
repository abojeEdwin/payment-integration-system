package com.paybridge.webhook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Webhook endpoints are PUBLIC (providers call them)
 * Security is handled via signature validation, not authentication
 */
@Configuration
@EnableWebSecurity
public class WebhookSecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable()) // Webhooks don't send CSRF tokens
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auths -> auths
						.requestMatchers("/webhooks/**").permitAll() // PUBLIC endpoints
						.requestMatchers("/actuator/health").permitAll()
						.anyRequest().denyAll() // Everything else blocked
				);
		return http.build();
	}
}