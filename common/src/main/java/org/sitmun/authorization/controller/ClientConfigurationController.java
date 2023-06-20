package org.sitmun.authorization.controller;

import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.dto.*;
import org.sitmun.authorization.service.ProfileService;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/config/client")
public class ClientConfigurationController {

  private final ProfileService profileService;

  /**
   * Constructor.
   */
  public ClientConfigurationController(ProfileService profileService) {
    this.profileService = profileService;
  }

  @GetMapping(path = "/application/{appId}/territories", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Page<TerritoryDtoLittle> getApplicationTerritories(@CurrentSecurityContext SecurityContext context, @PathVariable Integer appId, Pageable pageable) {
    String username = context.getAuthentication().getName();
    Page<Territory> page = profileService.getApplicationTerritories(username, appId, pageable);
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
  @ResponseBody
  public Page<ApplicationDtoLittle> getApplications(@CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    Page<Application> page = profileService.getApplications(username, pageable);
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
  @ResponseBody
  public Page<ApplicationDtoLittle> getTerritoryApplications(@CurrentSecurityContext SecurityContext context, @PathVariable Integer terrId, Pageable pageable) {
    String username = context.getAuthentication().getName();
    Page<Application> page = profileService.getTerritoryApplications(username, terrId, pageable);
    List<ApplicationDtoLittle> applications = Mappers.getMapper(ApplicationMapper.class).map(page.getContent());
    return new PageImpl<>(applications, page.getPageable(), page.getTotalElements());
  }

  @GetMapping(path = "/territory", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public Page<TerritoryDtoLittle> getTerritories(@CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    Page<Territory> page = profileService.getTerritories(username, pageable);
    List<TerritoryDtoLittle> territories = Mappers.getMapper(TerritoryMapper.class).map(page.getContent());
    return new PageImpl<>(territories, page.getPageable(), page.getTotalElements());
  }

  @GetMapping(path = "/profile/{appId}/{terrId}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<ProfileDto> getProfile(@CurrentSecurityContext SecurityContext context, @PathVariable("appId") String appId, @PathVariable("terrId") String terrId) {
    String username = context.getAuthentication().getName();
    return profileService.buildProfile(username, appId, terrId)
      .map(profile -> Mappers.getMapper(ProfileMapper.class).map(profile))
      .map(profile -> ResponseEntity.ok().body(profile))
      .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }
}
