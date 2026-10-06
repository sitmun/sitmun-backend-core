package org.sitmun.administration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.check.ServiceCheckRepository;
import org.sitmun.domain.service.check.ServiceCheckSampleRepository;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.sitmun.test.BaseTest;
import org.sitmun.upstream.http.BasicAuthorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@DisplayName("POST /api/services/{id}/access-check")
class ServiceAccessCheckTest extends BaseTest {

  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckRepository checkRepository;
  @Autowired private ServiceCheckSampleRepository sampleRepository;

  private MockWebServer server;

  @BeforeEach
  void startServer() throws Exception {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void stopServer() throws Exception {
    server.close();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("200 capabilities is up, with no credentials and an empty detail")
  void capabilities200IsUp() throws Exception {
    server.enqueue(response(200, "<WMS_Capabilities/>"));
    Service service = saveService("WMS", server.url("/wms").toString(), null, true);

    mvc.perform(post("/api/services/" + service.getId() + "/access-check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("up"))
        .andExpect(jsonPath("$.observer").value("backend"))
        .andExpect(jsonPath("$.detail").value(""))
        .andExpect(jsonPath("$.elapsedMs").isNumber())
        .andExpect(jsonPath("$.observedAt").isNotEmpty());

    RecordedRequest request = server.takeRequest();
    assertThat(request.getPath()).isEqualTo("/wms?request=GetCapabilities&service=WMS");
    assertThat(request.getHeader("Authorization")).isNull();
    assertThat(serviceRepository.findById(service.getId()).orElseThrow().getBlocked()).isTrue();
    assertThat(
            checkRepository
                .findByService_IdAndObserver(service.getId(), ServiceCheckStore.BACKEND)
                .orElseThrow()
                .getStatus())
        .isEqualTo("up");
    assertThat(
            sampleRepository.findByService_IdAndObserverOrderByIdAsc(
                service.getId(), ServiceCheckStore.BACKEND))
        .hasSize(1);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("401 is auth_failed and the detail keeps host and path only")
  void http401IsAuthFailed() throws Exception {
    server.enqueue(response(401, "no"));
    Service service = saveService("WMS", server.url("/wms").toString(), "s3cret", false);

    String body =
        mvc.perform(post("/api/services/" + service.getId() + "/access-check"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("auth_failed"))
            .andExpect(jsonPath("$.observer").value("backend"))
            .andExpect(
                jsonPath("$.detail")
                    .value(
                        "GetCapabilities "
                            + server.url("/wms").uri().getHost()
                            + "/wms | HTTP 401"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(body).doesNotContain("s3cret");
    assertThat(server.takeRequest().getHeader("Authorization"))
        .isEqualTo(BasicAuthorization.headerValue("user", "s3cret").orElseThrow());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("500 is server_error")
  void http500IsServerError() throws Exception {
    server.enqueue(response(500, "down"));
    Service service = saveService("WMS", server.url("/wms").toString(), null, false);

    mvc.perform(post("/api/services/" + service.getId() + "/access-check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("server_error"))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "GetCapabilities " + server.url("/wms").uri().getHost() + "/wms | HTTP 500"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("AIMS 200 is reached and the body is not an OGC status")
  void aims200IsReached() throws Exception {
    server.enqueue(
        response(200, "<ExceptionReport><ExceptionText>nope</ExceptionText></ExceptionReport>"));
    Service service = saveService("AIMS", server.url("/aims").toString(), null, false);

    mvc.perform(post("/api/services/" + service.getId() + "/access-check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("reached"))
        .andExpect(jsonPath("$.detail").value(""));

    assertThat(server.takeRequest().getPath()).isEqualTo("/aims");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("WMTS HTTP error tries the capabilities document once")
  void wmtsFallsBackAfterHttpError() throws Exception {
    server.enqueue(response(500, "down"));
    server.enqueue(response(200, "<Capabilities/>"));
    Service service = saveService("WMTS", server.url("/wmts").toString(), null, false);

    mvc.perform(post("/api/services/" + service.getId() + "/access-check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("up"))
        .andExpect(jsonPath("$.detail").value(""));

    assertThat(server.takeRequest().getPath())
        .isEqualTo("/wmts?request=GetCapabilities&service=WMTS");
    assertThat(server.takeRequest().getPath()).isEqualTo("/wmts/1.0.0/WMTSCapabilities.xml");
    assertThat(
            sampleRepository.findByService_IdAndObserverOrderByIdAsc(
                service.getId(), ServiceCheckStore.BACKEND))
        .hasSize(1);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("Unknown service is not found")
  void unknownServiceIsNotFound() throws Exception {
    mvc.perform(post("/api/services/99999999/access-check")).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Anonymous access is rejected")
  void anonymousIsUnauthorized() throws Exception {
    mvc.perform(post("/api/services/1/access-check")).andExpect(status().isUnauthorized());
  }

  private Service saveService(String type, String url, String password, boolean blocked) {
    return serviceRepository.save(
        Service.builder()
            .name(type + " probe")
            .type(type)
            .serviceURL(url)
            .blocked(blocked)
            .isProxied(false)
            .user(password == null ? null : "user")
            .password(password)
            .build());
  }

  private static MockResponse response(int code, String body) {
    return new MockResponse().setResponseCode(code).setBody(body);
  }
}
