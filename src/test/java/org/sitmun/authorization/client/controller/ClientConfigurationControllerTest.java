package org.sitmun.authorization.client.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.client.mapper.ProfileMapper;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.authorization.client.service.ClientUserPositionService;
import org.sitmun.authorization.client.service.MobileEditionAccessService;
import org.sitmun.authorization.client.service.Profile;
import org.sitmun.authorization.client.service.ProxyMiddlewareUrlResolver;
import org.sitmun.domain.application.Application;
import org.sitmun.infrastructure.persistence.type.i18n.DatabaseDefaultLanguageResolver;
import org.sitmun.infrastructure.persistence.type.i18n.TranslationRepository;
import org.sitmun.infrastructure.web.config.RequestLocaleResolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientConfigurationController.class)
@DisplayName("ClientConfigurationController REST test")
@AutoConfigureMockMvc(addFilters = false)
class ClientConfigurationControllerTest {

  @Autowired private MockMvc mvc;

  @MockitoBean private AuthorizationService authorizationService;

  @MockitoBean private ClientUserPositionService clientUserPositionService;

  @MockitoBean private ProfileMapper profileMapper;

  @MockitoBean private MobileEditionAccessService mobileEditionAccessService;

  @MockitoBean private TranslationRepository translationRepository;

  @MockitoBean private DatabaseDefaultLanguageResolver databaseDefaultLanguageResolver;

  @MockitoBean private RequestLocaleResolutionService requestLocaleResolutionService;

  @MockitoBean private CookieService cookieService;

  @MockitoBean private UserApplicationAccessPolicy userApplicationAccessPolicy;

  @MockitoBean private ProxyMiddlewareUrlResolver proxyMiddlewareUrlResolver;

  @Test
  @DisplayName("GET: ED application config must not expose mbtilesUrl")
  @WithMockUser(username = "testuser", roles = "USER")
  void getApplicationsDoesNotExposeMbtilesUrl() throws Exception {
    Application app = new Application();
    app.setId(1);
    app.setTitle("Edition Application");
    app.setType("ED");

    Page<Application> page = new PageImpl<>(Collections.singletonList(app));
    when(authorizationService.findApplicationsByUser(anyString(), any(Pageable.class)))
        .thenReturn(page);

    mvc.perform(get("/api/config/client/application"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].type").value("ED"))
        .andExpect(jsonPath("$.content[0].config.mbtilesUrl").doesNotExist());
  }

  @Test
  @DisplayName("GET: Multiple applications must not expose mbtilesUrl")
  @WithMockUser(username = "testuser", roles = "USER")
  void getApplicationsDoesNotExposeMbtilesUrlForMultipleApps() throws Exception {
    Application app1 = new Application();
    app1.setId(1);
    app1.setTitle("Test Application 1");
    app1.setType("T");

    Application app2 = new Application();
    app2.setId(2);
    app2.setTitle("Test Application 2");
    app2.setType("ED");

    Page<Application> page = new PageImpl<>(Arrays.asList(app1, app2));
    when(authorizationService.findApplicationsByUser(anyString(), any(Pageable.class)))
        .thenReturn(page);

    mvc.perform(get("/api/config/client/application"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].config.mbtilesUrl").doesNotExist())
        .andExpect(jsonPath("$.content[1].config.mbtilesUrl").doesNotExist());
  }

  @Test
  @WithMockUser(username = "testuser", roles = "USER")
  void getProfileReturnsForbiddenProblemWhenProfileIsUnavailable() throws Exception {
    when(authorizationService.createProfile(any())).thenReturn(Optional.<Profile>empty());

    mvc.perform(get("/api/config/client/profile/1/2"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("https://sitmun.org/problems/forbidden"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.detail").value("Access is denied"))
        .andExpect(jsonPath("$.instance").value("/api/config/client/profile/1/2"));
  }

  @Test
  @WithMockUser(username = "testuser", roles = "USER")
  void getProfileReturnsForbiddenProblemWhenApplicationAccessIsDenied() throws Exception {
    doThrow(new AccessDeniedException("Access denied to application"))
        .when(authorizationService)
        .ensureMayAccessApplication(1, "testuser");

    mvc.perform(get("/api/config/client/profile/1/2"))
        .andExpect(status().isForbidden())
        .andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value("https://sitmun.org/problems/forbidden"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.detail").value("Access is denied"))
        .andExpect(jsonPath("$.instance").value("/api/config/client/profile/1/2"));
  }
}
