package org.sitmun.authorization.proxy.controllers;

import static org.sitmun.infrastructure.security.jwt.MobileTokenScopes.PROXY_REQUEST;
import static org.sitmun.infrastructure.security.jwt.MobileTokenScopes.forMbtilesAction;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.mbtiles.MbtilesProxyConfigResponseDto;
import org.sitmun.authorization.proxy.mbtiles.MbtilesProxyRequestDto;
import org.sitmun.authorization.proxy.mbtiles.MbtilesResourceAccessValidator;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.ProxyDelegatedTokenAuthenticator;
import org.sitmun.authorization.proxy.service.ProxyDelegatedTokenAuthenticator.Failure;
import org.sitmun.authorization.proxy.service.ProxyDelegatedTokenAuthenticator.Outcome;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.infrastructure.web.dto.ProblemDetail;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/proxy")
@Slf4j
public class ProxyConfigurationController {

  private final ProxyConfigurationService proxyConfigurationService;
  private final ProxyDelegatedTokenAuthenticator delegatedTokenAuthenticator;
  private final MbtilesResourceAccessValidator mbtilesResourceAccessValidator;

  public ProxyConfigurationController(
      ProxyConfigurationService proxyConfigurationService,
      ProxyDelegatedTokenAuthenticator delegatedTokenAuthenticator,
      MbtilesResourceAccessValidator mbtilesResourceAccessValidator) {
    this.proxyConfigurationService = proxyConfigurationService;
    this.delegatedTokenAuthenticator = delegatedTokenAuthenticator;
    this.mbtilesResourceAccessValidator = mbtilesResourceAccessValidator;
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

    Outcome outcome =
        delegatedTokenAuthenticator.authenticate(
            delegatedTokenAuthenticator
                .extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION))
                .orElse(null),
            List.of(PROXY_REQUEST));

    if (outcome instanceof Outcome.Denied denied) {
      return unauthorizedForToken(denied.failure(), request);
    }

    String username;
    long expirationTime;
    if (outcome instanceof Outcome.Success success) {
      username = success.result().username();
      expirationTime = success.result().expirationTimeMillis();
    } else if (outcome instanceof Outcome.PublicPrincipal publicPrincipal) {
      username = publicPrincipal.result().username();
      expirationTime = 0;
    } else {
      return unauthorizedForToken(Failure.INVALID, request);
    }

    log.debug("Resolved proxy principal={}", username);
    if (proxyConfigurationService.validateUserAccess(configProxyRequestDto, username)) {
      log.info("User {} is authorized to access the requested configuration", username);
      try {
        RequestCoordinates coordinates =
            proxyConfigurationService.getRequestCoordinates(configProxyRequestDto, username);
        ConfigProxyDto configProxyDto =
            proxyConfigurationService.getConfiguration(
                configProxyRequestDto, expirationTime, coordinates);
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

  @PostMapping(path = "/mbtiles", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<?> getMbtilesConfiguration(
      @Valid @RequestBody MbtilesProxyRequestDto body, HttpServletRequest request) {
    String requiredScope = forMbtilesAction(body.action());
    if (requiredScope == null) {
      return problem(
          HttpStatus.BAD_REQUEST,
          ProblemTypes.BAD_REQUEST,
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          "Unsupported MBTiles action",
          request);
    }

    Outcome outcome =
        delegatedTokenAuthenticator.authenticate(
            delegatedTokenAuthenticator
                .extractBearer(request.getHeader(HttpHeaders.AUTHORIZATION))
                .orElse(null),
            List.of(requiredScope),
            true);

    if (outcome instanceof Outcome.Denied denied) {
      return unauthorizedForToken(denied.failure(), request);
    }
    if (outcome instanceof Outcome.PublicPrincipal) {
      return problem(
          HttpStatus.FORBIDDEN,
          ProblemTypes.FORBIDDEN,
          "Forbidden",
          "Public principal cannot access MBTiles configuration",
          request);
    }

    Outcome.Success success = (Outcome.Success) outcome;
    MbtilesResourceAccessValidator.Outcome access =
        mbtilesResourceAccessValidator.authorize(
            body, success.result().username(), success.result().expirationTimeMillis());

    if (access instanceof MbtilesResourceAccessValidator.Outcome.Denied denied) {
      return problem(
          HttpStatus.FORBIDDEN, ProblemTypes.FORBIDDEN, "Forbidden", denied.detail(), request);
    }

    MbtilesProxyConfigResponseDto response =
        ((MbtilesResourceAccessValidator.Outcome.Allowed) access).response();
    return ResponseEntity.ok(response);
  }

  private ResponseEntity<ProblemDetail> unauthorizedForToken(
      Failure failure, HttpServletRequest request) {
    String detail =
        switch (failure) {
          case EXPIRED -> "Proxy JWT is invalid or expired";
          case BLOCKED -> "Authentication is required";
          case WRONG_KIND -> "Proxy JWT is invalid or expired";
          case INVALID -> "Proxy JWT is invalid or expired";
        };
    return problem(
        HttpStatus.UNAUTHORIZED, ProblemTypes.UNAUTHORIZED, "Unauthorized", detail, request);
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
