package org.sitmun.authorization.proxy.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/proxy")
@Slf4j
public class ProxyConfigurationController {

  private final ProxyConfigurationService proxyConfigurationService;

  private final JsonWebTokenService jsonWebTokenService;

  public ProxyConfigurationController(
      ProxyConfigurationService proxyConfigurationService,
      JsonWebTokenService jsonWebTokenService) {
    this.proxyConfigurationService = proxyConfigurationService;
    this.jsonWebTokenService = jsonWebTokenService;
  }

  @PostMapping(produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<ConfigProxyDto> getServiceConfiguration(
      @RequestBody ConfigProxyRequestDto configProxyRequestDto) {
    log.info(
        "Requesting configuration for appId:{} terId:{} type:{} typeId:{}",
        configProxyRequestDto.getAppId(),
        configProxyRequestDto.getTerId(),
        configProxyRequestDto.getType(),
        configProxyRequestDto.getTypeId());
    String token = configProxyRequestDto.getToken();
    String username = null;
    long expirationTime = 0;
    if (StringUtils.hasText(token)) {
      log.info("Request has token");
      try {
        username = jsonWebTokenService.getUsernameFromToken(token);
        expirationTime = jsonWebTokenService.getExpirationDateFromToken(token).getTime();
      } catch (ExpiredJwtException e) {
        log.error("JWT is expired for user {}", username);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      } catch (Exception e) {
        log.error("JWT is invalid for user {}", username);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
      }
      log.info("Token identifies user {} with expiration time {}", username, expirationTime);
    } else {
      username = SecurityConstants.PUBLIC_PRINCIPAL;
      log.info("No token identifies user {} with expiration time {}", username, expirationTime);
    }
    if (proxyConfigurationService.validateUserAccess(configProxyRequestDto, username)) {
      log.info("User {} is authorized to access the requested configuration", username);
      try {
        ConfigProxyDto configProxyDto =
            proxyConfigurationService.getConfiguration(configProxyRequestDto, expirationTime);
        proxyConfigurationService.applyDecorators(configProxyDto, configProxyRequestDto, username);
        log.info("User {} is informed of the configuration", username);
        return ResponseEntity.ok().body(configProxyDto);
      } catch (Exception e) {
        log.error("Error getting configuration for user {}", username, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }
    }
    log.error(
        "Unauthorized: User {} is not authorized to access the requested configuration", username);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
