package org.sitmun.administration.service.extractor.capabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sitmun.administration.service.access.AccessProbePlan;
import org.sitmun.administration.service.access.AccessProbePlan.Authorization;
import org.sitmun.domain.service.Service;

@DisplayName("Access probe plan")
class AccessProbePlannerTest {

  @Test
  @DisplayName("WMS plan is GetCapabilities and ignores blocked and proxied")
  void wmsPlanIsGetCapabilities() {
    Service service = service("WMS", "http://example.test/wms");
    service.setBlocked(true);
    service.setIsProxied(false);

    AccessProbePlan plan = AccessProbePlanner.plan(service);

    assertThat(plan.protocol()).isEqualTo("WMS");
    assertThat(plan.request()).isEqualTo("GetCapabilities");
    assertThat(plan.interpretBody()).isTrue();
    assertThat(plan.fallbackUrl()).isNull();
    assertThat(plan.url())
        .hasToString("http://example.test/wms?request=GetCapabilities&service=WMS");
    assertThat(plan.authorization()).isInstanceOf(Authorization.None.class);
  }

  @Test
  @DisplayName("WMS plan keeps an existing GetCapabilities URL")
  void wmsPlanKeepsExistingGetCapabilitiesUrl() {
    Service service =
        service("WMS", "http://example.test/wms?request=GetCapabilities&version=1.3.0");

    AccessProbePlan plan = AccessProbePlanner.plan(service);

    assertThat(plan.url())
        .hasToString("http://example.test/wms?request=GetCapabilities&version=1.3.0");
  }

  @Test
  @DisplayName("WFS plan is GetCapabilities on the service URL")
  void wfsPlanIsGetCapabilities() {
    AccessProbePlan plan = AccessProbePlanner.plan(service("WFS", "http://example.test/wfs"));

    assertThat(plan.request()).isEqualTo("GetCapabilities");
    assertThat(plan.interpretBody()).isTrue();
    assertThat(plan.fallbackUrl()).isNull();
    assertThat(plan.url())
        .hasToString("http://example.test/wfs?request=GetCapabilities&service=WFS");
  }

  @Test
  @DisplayName("WMTS plan carries the fallback URL and does not open a socket")
  void wmtsPlanCarriesFallbackUrl() {
    AccessProbePlan plan = AccessProbePlanner.plan(service("WMTS", "http://example.test/wmts"));

    assertThat(plan.request()).isEqualTo("GetCapabilities");
    assertThat(plan.interpretBody()).isTrue();
    assertThat(plan.url())
        .hasToString("http://example.test/wmts?request=GetCapabilities&service=WMTS");
    assertThat(plan.fallbackUrl())
        .hasToString("http://example.test/wmts/1.0.0/WMTSCapabilities.xml");
  }

  @ParameterizedTest
  @ValueSource(strings = {"AIMS", "FME", "TC"})
  @DisplayName("AIMS, FME, and TC plans are GET with body interpretation off")
  void nonOgcPlanIsGet(String type) {
    AccessProbePlan plan = AccessProbePlanner.plan(service(type, "http://example.test/service"));

    assertThat(plan.protocol()).isEqualTo(type);
    assertThat(plan.request()).isEqualTo("GET");
    assertThat(plan.interpretBody()).isFalse();
    assertThat(plan.fallbackUrl()).isNull();
    assertThat(plan.url()).hasToString("http://example.test/service");
  }

  @Test
  @DisplayName("Basic auth is planned only when the password is set")
  void basicAuthOnlyWhenPasswordSet() {
    Service withPassword = service("WMS", "http://alice:s3cret@example.test/wms");
    withPassword.setUser("alice");
    withPassword.setPassword("s3cret");
    withPassword.setAuthenticationMode("none");

    AccessProbePlan planned = AccessProbePlanner.plan(withPassword);

    assertThat(planned.url())
        .hasToString("http://example.test/wms?request=GetCapabilities&service=WMS");
    assertThat(planned.authorization()).isEqualTo(new Authorization.Basic("alice", "s3cret"));
    assertThat(planned.toString()).doesNotContain("s3cret");

    Service withoutPassword = service("WMS", "http://example.test/wms");
    withoutPassword.setUser("alice");
    withoutPassword.setPassword("");
    withoutPassword.setAuthenticationMode("basic");

    assertThat(AccessProbePlanner.plan(withoutPassword).authorization())
        .isInstanceOf(Authorization.None.class);
  }

  @Test
  @DisplayName("A non-WMS type is not sent through the WMS capabilities builder")
  void lowercaseWmsIsRejected() {
    assertThatThrownBy(() -> AccessProbePlanner.plan(service("wms", "http://example.test/wms")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported service type for access check: wms");
  }

  private static Service service(String type, String url) {
    return Service.builder()
        .name(type)
        .type(type)
        .serviceURL(url)
        .blocked(false)
        .isProxied(true)
        .build();
  }
}
