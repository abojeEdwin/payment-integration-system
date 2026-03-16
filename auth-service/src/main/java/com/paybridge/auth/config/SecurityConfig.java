package com.paybridge.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtTokenProvider   jwtTokenProvider;
	private final ApiKeyFilter       apiKeyFilter;
	private final UserDetailsService userDetailsService;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auths -> auths
						// Public endpoints
						.requestMatchers("/auth/login", "/auth/register").permitAll()
						.requestMatchers("/actuator/**").permitAll()

						// Merchant admin endpoints (require JWT)
						.requestMatchers("/auth/**", "/api-keys/**").authenticated()

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

	// ✅ Password encoder bean (required for BCrypt)
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// ✅ CRITICAL FIX: Expose AuthenticationManager bean
	@Bean
	public AuthenticationManager authenticationManager(
			AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	// ✅ DaoAuthenticationProvider (links UserDetailsService + PasswordEncoder)
	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());
		return authProvider;
	}
}