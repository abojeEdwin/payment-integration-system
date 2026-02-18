package com.paybridge.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider jwtTokenProvider;
	private final ApiKeyFilter apiKeyFilter;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(authz -> authz
						// Public endpoints
						.requestMatchers("/auth/login").permitAll()
						.requestMatchers("/actuator/**").permitAll()

						// Merchant admin endpoints (require JWT)
						.requestMatchers("/auth/merchants/**").authenticated()
						.requestMatchers("/api-keys/**").authenticated()

						// Everything else denied
						.anyRequest().denyAll()
				)
				// Add JWT filter for authenticated endpoints
				.addFilterBefore(new JwtTokenFilter(jwtTokenProvider),
						UsernamePasswordAuthenticationFilter.class)
				// Add API key filter for all requests (logging/audit)
				.addFilterBefore(apiKeyFilter, JwtTokenFilter.class);

		return http.build();
	}
}