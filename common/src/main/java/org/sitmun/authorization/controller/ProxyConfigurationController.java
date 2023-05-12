package org.sitmun.authorization.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.sitmun.authorization.dto.ConfigProxyDto;
import org.sitmun.authorization.dto.ConfigProxyRequest;
import org.sitmun.authorization.service.ProxyConfigurationService;
import org.sitmun.infrastructure.security.config.WebSecurityConfigurer;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/proxy")
public class ProxyConfigurationController {
	
	private final ProxyConfigurationService proxyConfigurationService;
	
	private final JsonWebTokenService jsonWebTokenService;
	
	public ProxyConfigurationController(ProxyConfigurationService proxyConfigurationService, JsonWebTokenService jsonWebTokenService) {
		this.proxyConfigurationService = proxyConfigurationService;
		this.jsonWebTokenService = jsonWebTokenService;
	}
	
	@PostMapping(produces = APPLICATION_JSON_VALUE)
	public ResponseEntity<ConfigProxyDto> getServiceConfiguration(@RequestBody ConfigProxyRequest configProxyRequest) {
		String username = configProxyRequest.getToken() != null ? jsonWebTokenService.getUsernameFromToken(configProxyRequest.getToken()): WebSecurityConfigurer.PUBLIC_USER_NAME;
		if(proxyConfigurationService.validateUserAccess(configProxyRequest, username)) {
			ConfigProxyDto configProxyDto = proxyConfigurationService.getConfiguration(configProxyRequest, username);
			return ResponseEntity.ok().body(configProxyDto);
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}
}
