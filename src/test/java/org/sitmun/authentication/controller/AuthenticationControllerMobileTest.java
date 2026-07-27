package org.sitmun.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.infrastructure.security.jwt.MobileJwtClaims;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Mobile authentication and client access")
class AuthenticationControllerMobileTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private JsonWebTokenService jsonWebTokenService;

  @BeforeEach
  void ensureEditionApplication() {
    Application app = applicationRepository.findById(1).orElseThrow();
    app.setType("ED");
    applicationRepository.saveAndFlush(app);
  }

  @Test
  @DisplayName("POST /authenticate/mobile returns Bearer JSON without cookies")
  void mobileLoginReturnsBearerToken() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    MvcResult result =
        mvc.perform(
                post("/api/authenticate/mobile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtils.asJsonString(login)))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.access_token").isNotEmpty())
            .andExpect(jsonPath("$.expires_in").value(3600))
            .andExpect(
                cookie().doesNotExist(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME))
            .andExpect(
                cookie().doesNotExist(AuthenticationController.ADMIN_ACCESS_TOKEN_COOKIE_NAME))
            .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    var claims = jsonWebTokenService.parseClaims(body.get("access_token").asText());
    assertThat(claims.getAudience()).contains(MobileJwtClaims.AUDIENCE_MOBILE_API);
    assertThat(claims.get(MobileJwtClaims.TOKEN_USE, String.class))
        .isEqualTo(MobileJwtClaims.TOKEN_USE_EDITION_ACCESS);
  }

  @Test
  @DisplayName("POST /authenticate/mobile returns 401 for bad credentials")
  void mobileLoginBadCredentials() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("wrong");

    mvc.perform(
            post("/api/authenticate/mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /authenticate/mobile returns 403 when user has no ED application")
  void mobileLoginForbiddenWithoutEditionApp() throws Exception {
    applicationRepository
        .findAll()
        .forEach(
            app -> {
              app.setType("I");
              applicationRepository.save(app);
            });
    applicationRepository.flush();

    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    mvc.perform(
            post("/api/authenticate/mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(login)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /authenticate remains cookie-only and never returns JSON token")
  void viewerLoginRemainsCookieOnly() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");

    MvcResult result =
        mvc.perform(
                post("/api/authenticate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtils.asJsonString(login)))
            .andExpect(status().isOk())
            .andExpect(cookie().exists(AuthenticationController.VIEWER_ACCESS_TOKEN_COOKIE_NAME))
            .andReturn();

    assertThat(result.getResponse().getContentAsString()).isEmpty();
  }

  @Test
  @DisplayName("Mobile Bearer can list applications and only receives ED apps")
  void mobileBearerListsOnlyEditionApplications() throws Exception {
    Application touristic = applicationRepository.findById(6).orElseThrow();
    touristic.setType("T");
    applicationRepository.saveAndFlush(touristic);

    String token = mobileAccessToken();

    mvc.perform(
            get("/api/config/client/application")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.type != 'ED')]").isEmpty())
        .andExpect(jsonPath("$.content[0].type").value("ED"))
        .andExpect(jsonPath("$.content[0].config.mbtilesUrl").doesNotExist());
  }

  @Test
  @DisplayName("Mobile Bearer is denied account and non-ED profile endpoints")
  void mobileBearerDenials() throws Exception {
    String token = mobileAccessToken();

    mvc.perform(get("/api/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());

    Application nonEdition = applicationRepository.findById(2).orElseThrow();
    nonEdition.setType("I");
    applicationRepository.saveAndFlush(nonEdition);

    mvc.perform(
            get("/api/config/client/profile/2/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());

    mvc.perform(
            get("/api/config/client/territory")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Mobile Bearer can exchange proxy_token via /authenticate/proxy")
  void mobileBearerProxyExchange() throws Exception {
    String accessToken = mobileAccessToken();

    MvcResult result =
        mvc.perform(
                post("/api/authenticate/proxy")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.proxy_token").isNotEmpty())
            .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    var claims = jsonWebTokenService.parseClaims(body.get("proxy_token").asText());
    assertThat(claims.getAudience()).contains(MobileJwtClaims.AUDIENCE_PROXY);
    assertThat(claims.get(MobileJwtClaims.TOKEN_USE, String.class))
        .isEqualTo(MobileJwtClaims.TOKEN_USE_MOBILE_PROXY_ACCESS);
  }

  private String mobileAccessToken() throws Exception {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");
    MvcResult result =
        mvc.perform(
                post("/api/authenticate/mobile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(TestUtils.asJsonString(login)))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("access_token")
        .asText();
  }
}
