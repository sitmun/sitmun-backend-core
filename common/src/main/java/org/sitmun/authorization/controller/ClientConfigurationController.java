package org.sitmun.authorization.controller;

import com.fasterxml.jackson.annotation.JsonView;
import org.sitmun.authorization.dto.JsonViewPage;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.authorization.service.ClientConfigurationService;
import org.sitmun.domain.application.Application;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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
   * @param context security context
   * @param pageable pagination information
   * @return a page of a list of applications
   */
  @GetMapping(path = "/application", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  public JsonViewPage<Application> getApplications(@CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    return JsonViewPage.of(clientConfigurationService.getApplications(username, pageable));
  }
}
