package org.sitmun.authorization.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.service.ClientConfigurationService;
import org.sitmun.domain.application.Application;
import org.sitmun.test.BaseTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.sitmun.infrastructure.security.config.WebSecurityConfigurer.PUBLIC_USER_NAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Client configuration controller test")
class ClientConfigurationControllerTest extends BaseTest {

  @MockBean
  private ClientConfigurationService service;

  @Test
  @DisplayName("Return public applications for anonymous authentication (ROLE_PUBLIC)")
  void listPublicApplications() throws Exception {

    when(service.getApplications(eq(PUBLIC_USER_NAME), any())).thenAnswer( request -> {
        PageRequest pageRequest = request.getArgument(1);
        List<Application> applications = new ArrayList<>();
        applications.add(Application.builder().name("public-app-1").build());
        applications.add(Application.builder().name("public-app-2").build());
        return new PageImpl<>(applications, pageRequest, applications.size());
      });

    mvc.perform(get("/api/config/client/application"))
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("public-app-1", "public-app-2")));
  }

  @Test
  @DisplayName("Return private applications for ROLE_USER")
  @WithMockUser(username= "other")
  void listPrivateApplications() throws Exception {

    when(service.getApplications(eq("other"), any())).thenAnswer( request -> {
      PageRequest pageRequest = request.getArgument(1);
      List<Application> applications = new ArrayList<>();
      applications.add(Application.builder().name("private-app-1").build());
      applications.add(Application.builder().name("private-app-2").build());
      return new PageImpl<>(applications, pageRequest, applications.size());
    });

    mvc.perform(get("/api/config/client/application"))
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.content").isArray())
      .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("private-app-1", "private-app-2")));
  }
}
