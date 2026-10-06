package org.sitmun.authorization.proxy.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.check.ServiceCheck;
import org.sitmun.domain.service.check.ServiceCheckRepository;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("POST /api/config/proxy/service-checks")
class ProxyServiceCheckControllerTest {

  @Autowired private MockMvc mvc;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ServiceCheckRepository checkRepository;

  @Value("${sitmun.proxy-middleware.secret}")
  private String proxySecret;

  @Test
  @DisplayName("Rejects a missing proxy key")
  void rejectsAMissingProxyKey() throws Exception {
    Service service = saveService();

    mvc.perform(
            post("/api/config/proxy/service-checks")
                .contentType(APPLICATION_JSON)
                .content(body(service.getId(), 500)))
        .andExpect(status().isForbidden());

    assertThat(
            checkRepository.findByService_IdAndObserver(service.getId(), ServiceCheckStore.PROXY))
        .isEmpty();
  }

  @Test
  @DisplayName("Upserts observer proxy")
  void upsertsObserverProxy() throws Exception {
    Service service = saveService();

    mvc.perform(
            post("/api/config/proxy/service-checks")
                .contentType(APPLICATION_JSON)
                .header(SecurityConstants.PROXY_MIDDLEWARE_KEY, proxySecret)
                .content(body(service.getId(), 500)))
        .andExpect(status().isNoContent());

    ServiceCheck saved =
        checkRepository
            .findByService_IdAndObserver(service.getId(), ServiceCheckStore.PROXY)
            .orElseThrow();
    assertThat(saved.getStatus()).isEqualTo("server_error");
    assertThat(saved.getStatusRank()).isEqualTo(50);
    assertThat(saved.getObserver()).isEqualTo("proxy");
    assertThat(saved.getElapsedMs()).isEqualTo(15L);
  }

  private Service saveService() {
    return serviceRepository.save(
        Service.builder()
            .name("Proxied WMS")
            .type("WMS")
            .serviceURL("http://maps.example/wms")
            .blocked(false)
            .isProxied(true)
            .build());
  }

  private static String body(Integer serviceId, int httpStatus) {
    return """
        {
          "serviceId": %d,
          "requestContext": {
            "protocol": "WMS",
            "request": "GetMap",
            "probe": false,
            "host": "maps.example",
            "path": "/wms"
          },
          "exchangeSignals": {
            "transportError": null,
            "transportMessage": "",
            "httpStatus": %d,
            "ogcExceptionCode": null,
            "ogcExceptionText": ""
          },
          "elapsedMs": 15,
          "observedAt": "2026-10-05T11:30:00Z"
        }
        """
        .formatted(serviceId, httpStatus);
  }
}
