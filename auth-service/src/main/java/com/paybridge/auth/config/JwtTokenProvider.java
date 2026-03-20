package com.paybridge.auth.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtTokenProvider {

	private final SecretKey secretKey;
	private final long      expirationMs;

	public JwtTokenProvider(@Value("${jwt.secret}") String secret,
							@Value("${jwt.expiration}") long expirationMs) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	/**
	 * Generate JWT token for merchant admin.
	 * The subject is the merchant UUID; email is embedded as a claim for
	 * audit/display purposes without being used as the identity key.
	 */
	public String generateToken(Authentication authentication, String email) {
		String merchantId = authentication.getName();

		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expirationMs);

		Map<String, Object> claims = new HashMap<>();
		claims.put("merchantId", merchantId);
		claims.put("email", email);

		return Jwts.builder()
				.claims(claims)
				.subject(merchantId)
				.issuedAt(now)
				.expiration(expiryDate)
				.signWith(secretKey, Jwts.SIG.HS512)
				.compact();
	}

	/**
	 * Extract merchant ID from token
	 */
	public String getMerchantIdFromToken(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		return claims.getSubject();
	}

	/**
	 * Validate token
	 */
	public boolean validateToken(String token) {
		try {
			Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token);
			return true;
		} catch (SecurityException ex) {
			throw new JwtException("Invalid JWT signature");
		} catch (MalformedJwtException ex) {
			throw new JwtException("Invalid JWT token");
		} catch (ExpiredJwtException ex) {
			throw new JwtException("JWT token is expired");
		} catch (UnsupportedJwtException ex) {
			throw new JwtException("JWT token is unsupported");
		} catch (IllegalArgumentException ex) {
			throw new JwtException("JWT claims string is empty");
		}
	}
}