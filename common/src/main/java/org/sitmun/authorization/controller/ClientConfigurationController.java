package org.sitmun.authorization.controller;

import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.dto.ApplicationDtoLittle;
import org.sitmun.authorization.dto.ApplicationMapper;
import org.sitmun.authorization.dto.ProfileDto;
import org.sitmun.authorization.dto.ProfileMapper;
import org.sitmun.authorization.service.ClientConfigurationService;
import org.sitmun.domain.application.Application;
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

  private final ClientConfigurationService clientConfigurationService;

  /**
   * Constructor.
   */
  public ClientConfigurationController(
    ClientConfigurationService clientConfigurationService) {
    this.clientConfigurationService = clientConfigurationService;
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
    Page<Application> page = clientConfigurationService.getApplications(username, pageable);
    List<ApplicationDtoLittle> applications = Mappers.getMapper(ApplicationMapper.class).map(page.getContent());
    return new PageImpl<>(applications, page.getPageable(), page.getTotalElements());
  }

  @GetMapping(path = "/profile/{appId}/{terrId}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<ProfileDto> getProfile(@CurrentSecurityContext SecurityContext context, @PathVariable("appId") String appId, @PathVariable("terrId") String terrId) {
    String username = context.getAuthentication().getName();
    return clientConfigurationService.getProfile(username, appId, terrId)
      .map(profile -> Mappers.getMapper(ProfileMapper.class).map(profile))
      .map(profile -> ResponseEntity.ok().body(profile))
      .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }
}
