package org.sitmun.authorization.proxy.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.infrastructure.web.dto.ProblemDetail;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

  private final UserApplicationAccessPolicy userApplicationAccessPolicy;

  public ProxyConfigurationController(
      ProxyConfigurationService proxyConfigurationService,
      JsonWebTokenService jsonWebTokenService,
      UserApplicationAccessPolicy userApplicationAccessPolicy) {
    this.proxyConfigurationService = proxyConfigurationService;
    this.jsonWebTokenService = jsonWebTokenService;
    this.userApplicationAccessPolicy = userApplicationAccessPolicy;
  }

  @PostMapping(produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getServiceConfiguration(
      @RequestBody ConfigProxyRequestDto configProxyRequestDto, HttpServletRequest request) {
    log.info(
        "Requesting configuration for appId:{} terId:{} type:{} typeId:{}",
        configProxyRequestDto.getAppId(),
        configProxyRequestDto.getTerId(),
        configProxyRequestDto.getType(),
        configProxyRequestDto.getTypeId());
    String token = configProxyRequestDto.getToken();
    boolean idTokenPresent = StringUtils.hasText(token);
    log.debug("Proxy config POST idTokenPresent={}", idTokenPresent);
    String username = null;
    long expirationTime = 0;
    if (idTokenPresent) {
      try {
        username = jsonWebTokenService.getUsernameFromToken(token);
        expirationTime = jsonWebTokenService.getExpirationDateFromToken(token).getTime();
      } catch (ExpiredJwtException e) {
        log.error("JWT is expired for user {}", username);
        return problem(
            HttpStatus.UNAUTHORIZED,
            ProblemTypes.UNAUTHORIZED,
            "Unauthorized",
            "Proxy JWT is invalid or expired",
            request);
      } catch (JwtException | IllegalArgumentException e) {
        log.error("JWT is invalid for user {}", username);
        return problem(
            HttpStatus.UNAUTHORIZED,
            ProblemTypes.UNAUTHORIZED,
            "Unauthorized",
            "Proxy JWT is invalid or expired",
            request);
      }
      log.info("Token identifies user {} with expiration time {}", username, expirationTime);
      if (userApplicationAccessPolicy.isBlockedAccount(username)) {
        return problem(
            HttpStatus.UNAUTHORIZED,
            ProblemTypes.UNAUTHORIZED,
            "Unauthorized",
            "Authentication is required",
            request);
      }
    } else {
      username = SecurityConstants.PUBLIC_PRINCIPAL;
      log.debug("Resolved public principal={}", username);
    }
    if (proxyConfigurationService.validateUserAccess(configProxyRequestDto, username)) {
      log.info("User {} is authorized to access the requested configuration", username);
      try {
        RequestCoordinates coordinates =
            proxyConfigurationService.getRequestCoordinates(configProxyRequestDto, username);
        ConfigProxyDto configProxyDto =
            proxyConfigurationService.getConfiguration(
                configProxyRequestDto, expirationTime, coordinates);
        log.debug(
            "Returning proxy configuration: configType={} exp={} payloadClass={}",
            configProxyDto.getType(),
            configProxyDto.getExp(),
            configProxyDto.getPayload() != null
                ? configProxyDto.getPayload().getClass().getSimpleName()
                : "null");
        proxyConfigurationService.applyDecorators(
            configProxyDto, configProxyRequestDto, coordinates);
        log.info("User {} is informed of the configuration", username);
        return ResponseEntity.ok().body(configProxyDto);
      } catch (BadRequestException e) {
        log.info("Invalid proxy configuration request for user {}: {}", username, e.getMessage());
        return problem(
            HttpStatus.BAD_REQUEST,
            ProblemTypes.BAD_REQUEST,
            HttpStatus.BAD_REQUEST.getReasonPhrase(),
            e.getMessage(),
            request);
      } catch (Exception e) {
        log.error("Error getting configuration for user {}", username, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
      }
    }
    log.error(
        "Unauthorized: User {} is not authorized to access the requested configuration", username);
    return problem(
        HttpStatus.FORBIDDEN,
        ProblemTypes.FORBIDDEN,
        "Forbidden",
        "The identified principal cannot access the requested proxy configuration",
        request);
  }

  private static ResponseEntity<ProblemDetail> problem(
      HttpStatus status, String type, String title, String detail, HttpServletRequest request) {
    ProblemDetail problem =
        ProblemDetail.builder()
            .type(type)
            .status(status.value())
            .title(title)
            .detail(detail)
            .instance(request.getRequestURI())
            .build();
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }
}
