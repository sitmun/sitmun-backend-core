package org.sitmun.authorization.controller;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.dto.*;
import org.sitmun.authorization.service.AuthorizationService;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.function.Function;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/config/client")
@Slf4j
public class ClientConfigurationController {

  private final AuthorizationService authorizationService;
  private final ProfileMapper profileMapper;

  @Value("${sitmun.proxy.force:false}")
  private boolean proxyForce;
  @Value("${sitmun.proxy.url:}")
  private String proxyUrl;

  /**
   * Constructor.
   */
  public ClientConfigurationController(AuthorizationService authorizationService, ProfileMapper profileMapper) {
    this.authorizationService = authorizationService;
    this.profileMapper = profileMapper;
  }

  @GetMapping(path = "/application/{appId}/territories", produces = APPLICATION_JSON_VALUE)
  public Page<TerritoryDtoLittle> getApplicationTerritories(@CurrentSecurityContext SecurityContext context, @PathVariable Integer appId, Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("name")));
    }
    Page<Territory> page = authorizationService.findTerritoriesByUserAndApplication(username, appId, pageable);
    List<TerritoryDtoLittle> territories = Mappers.getMapper(TerritoryMapper.class).map(page.getContent());
    return new PageImpl<>(territories, page.getPageable(), page.getTotalElements());
  }

  /**
   * Get the list of applications.
   *
   * @param context  security context
   * @param pageable pagination information
   * @return a page of a list of applications
   */
  @GetMapping(path = "/application", produces = APPLICATION_JSON_VALUE)
  public Page<ApplicationDtoLittle> getApplications(@CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("title")));
    }
    Page<Application> page = authorizationService.findApplicationsByUser(username, pageable);
    List<ApplicationDtoLittle> applications = Mappers.getMapper(ApplicationMapper.class).map(page.getContent());
    return new PageImpl<>(applications, page.getPageable(), page.getTotalElements());
  }

  /**
   * Get the list of applications in a territory.
   *
   * @param context  security context
   * @param pageable pagination information
   * @return a page of a list of applications
   */
  @GetMapping(path = "/territory/{terrId}/applications", produces = APPLICATION_JSON_VALUE)
  public Page<ApplicationDtoLittle> getTerritoryApplications(@CurrentSecurityContext SecurityContext context, @PathVariable Integer terrId, Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("title")));
    }
    Page<Application> page = authorizationService.findApplicationsByUserAndTerritory(username, terrId, pageable);
    List<ApplicationDtoLittle> applications = Mappers.getMapper(ApplicationMapper.class).map(page.getContent());
    return new PageImpl<>(applications, page.getPageable(), page.getTotalElements());
  }

  @GetMapping(path = "/territory", produces = APPLICATION_JSON_VALUE)
  public Page<TerritoryDtoLittle> getTerritories(@CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("name")));
    }
    Page<Territory> page = authorizationService.findTerritoriesByUser(username, pageable);
    List<TerritoryDtoLittle> territories = Mappers.getMapper(TerritoryMapper.class).map(page.getContent());
    return new PageImpl<>(territories, page.getPageable(), page.getTotalElements());
  }

  @GetMapping(path = "/profile/{appId}/{terrId}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<ProfileDto> getProfile(@CurrentSecurityContext SecurityContext context, @PathVariable("appId") Integer appId, @PathVariable("terrId") Integer terrId) {
    String username = context.getAuthentication().getName();
    return authorizationService.createProfile(username, appId, terrId)
      .map(profileMapper::map)
      .map(getProfileDtoProfileDtoFunction(appId, terrId))
      .map(profile -> ResponseEntity.ok().body(profile))
      .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  private Function<ProfileDto, ProfileDto> getProfileDtoProfileDtoFunction(Integer appId, Integer terrId) {
    return profile -> {
      profile.getServices().forEach(service -> {
        if (proxyForce || Boolean.TRUE.equals(service.getIsProxied())) {
          service.setIsProxied(true);
          String uriTemplate = proxyUrl + "/proxy/{appId}/{terId}/{type}/{typeId}";
          log.info("Creating forced proxy URL for appId:{} terId:{} type:{} typeId:{} with template:{}",
            appId,
            terrId,
            service.getType(),
            service.getId().substring(8),
            uriTemplate);
          String uri = UriComponentsBuilder.fromUriString(uriTemplate)
            .build(appId, terrId, service.getType(), service.getId().substring(8))
            .toString();
          service.setUrl(uri);
        }
      });
      return profile;
    };
  }
}
