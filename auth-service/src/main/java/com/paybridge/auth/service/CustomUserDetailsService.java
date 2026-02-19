package com.paybridge.auth.service;

import com.paybridge.auth.entity.Merchant;
import com.paybridge.auth.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	    private final MerchantRepository merchantRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		Merchant merchant = merchantRepository.findActiveByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("Merchant not found: " + email));

		// Map merchant to Spring Security UserDetails
		return new User(
				merchant.getEmail(),              // username = email
				merchant.getPasswordHash(),       // BCrypt hashed password
				List.of(new SimpleGrantedAuthority("ROLE_MERCHANT")) // authorities
		);
	}
}
