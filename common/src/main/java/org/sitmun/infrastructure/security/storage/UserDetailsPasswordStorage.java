package org.sitmun.infrastructure.security.storage;

import org.sitmun.infrastructure.security.core.userdetails.UserDetailsServiceImplementation;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order
public class UserDetailsPasswordStorage implements PasswordStorage {

	private final UserDetailsServiceImplementation userDetailsService;
	
	public UserDetailsPasswordStorage(UserDetailsServiceImplementation userDetailsService) {
		this.userDetailsService = userDetailsService;
	}
	  
	@Override
	public void addPasswordStorage(AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
		authenticationManagerBuilder.userDetailsService(userDetailsService)
		  .passwordEncoder(passwordEncoder());
	}

    public PasswordEncoder passwordEncoder() {
	   return new BCryptPasswordEncoder();
	}
}