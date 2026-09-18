package org.sitmun.administration.service.extractor.capabilities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import okhttp3.Credentials;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.administration.controller.dto.ServiceCapabilitiesRequest;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServiceCapabilitiesProbeService")
class ServiceCapabilitiesProbeServiceTest {

  @Mock private ServiceRepository serviceRepository;
  @Mock private ServiceCapabilitiesExtractor extractor;

  private ServiceCapabilitiesProbeService probeService;

  @BeforeEach
  void setUp() {
    probeService = new ServiceCapabilitiesProbeService(serviceRepository, List.of(extractor));
  }

  @Test
  @DisplayName("Builds GetCapabilities from a form URL and type without Authorization")
  void buildsGetCapabilitiesWithoutAuthorization() {
    when(extractor.extract(any(Request.class))).thenReturn(success());
    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setUrl("https://example.com/wms?map=/maps/demo.map");
    request.setType("WMS");
    request.setAuthenticationMode("None");

    ExtractedMetadata result = probeService.probe(request);

    assertThat(result.getSuccess()).isTrue();
    Request httpRequest = capturedRequest();
    assertThat(httpRequest.url().queryParameter("map")).isEqualTo("/maps/demo.map");
    assertThat(httpRequest.url().queryParameter("request")).isEqualTo("GetCapabilities");
    assertThat(httpRequest.url().queryParameter("service")).isEqualTo("WMS");
    assertThat(httpRequest.header("Authorization")).isNull();
  }

  @Test
  @DisplayName("Adds HTTP Basic when mode is HTTP Basic authentication and password is present")
  void addsHttpBasicAuthorization() {
    when(extractor.extract(any(Request.class))).thenReturn(success());
    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setUrl("https://example.com/wms");
    request.setType("WMS");
    request.setAuthenticationMode("HTTP Basic authentication");
    request.setUser("alice");
    request.setPassword("secret");

    probeService.probe(request);

    Request httpRequest = capturedRequest();
    assertThat(httpRequest.header("Authorization")).isEqualTo(Credentials.basic("alice", "secret"));
  }

  @Test
  @DisplayName("Does not send Basic when authenticationMode is None")
  void skipsBasicWhenModeIsNone() {
    when(extractor.extract(any(Request.class))).thenReturn(success());
    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setUrl("https://example.com/wms");
    request.setType("WMS");
    request.setAuthenticationMode("None");
    request.setUser("alice");
    request.setPassword("secret");

    probeService.probe(request);

    assertThat(capturedRequest().header("Authorization")).isNull();
  }

  @Test
  @DisplayName("Treats id < 1 as omit and requires url and type")
  void treatsNegativeIdAsOmit() {
    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setId(-1);
    request.setUrl("https://example.com/wms");
    request.setType("WMS");
    when(extractor.extract(any(Request.class))).thenReturn(success());

    probeService.probe(request);

    verify(extractor).extract(any(Request.class));
  }

  @Test
  @DisplayName("Overlays present fields on a stored service and keeps omitted password")
  void overlaysStoredServiceAndKeepsOmittedPassword() {
    Service stored =
        Service.builder()
            .id(12)
            .serviceURL("https://stored.example/wms")
            .type("WMS")
            .authenticationMode("HTTP Basic authentication")
            .user("stored-user")
            .password("stored-pass")
            .blocked(false)
            .build();
    when(serviceRepository.findById(12)).thenReturn(Optional.of(stored));
    when(extractor.extract(any(Request.class))).thenReturn(success());

    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setId(12);
    request.setUrl("https://form.example/wms");
    request.setType("WMS");
    request.setAuthenticationMode("HTTP Basic authentication");
    request.setUser("form-user");

    probeService.probe(request);

    Request httpRequest = capturedRequest();
    assertThat(httpRequest.url().host()).isEqualTo("form.example");
    assertThat(httpRequest.header("Authorization"))
        .isEqualTo(Credentials.basic("form-user", "stored-pass"));
  }

  @Test
  @DisplayName("JSON password overlays the stored password")
  void jsonPasswordOverlaysStoredPassword() {
    Service stored =
        Service.builder()
            .id(12)
            .serviceURL("https://stored.example/wms")
            .type("WMS")
            .authenticationMode("HTTP Basic authentication")
            .user("stored-user")
            .password("stored-pass")
            .blocked(false)
            .build();
    when(serviceRepository.findById(12)).thenReturn(Optional.of(stored));
    when(extractor.extract(any(Request.class))).thenReturn(success());

    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setId(12);
    request.setUrl("https://stored.example/wms");
    request.setType("WMS");
    request.setAuthenticationMode("HTTP Basic authentication");
    request.setUser("stored-user");
    request.setPassword("typed-pass");

    probeService.probe(request);

    assertThat(capturedRequest().header("Authorization"))
        .isEqualTo(Credentials.basic("stored-user", "typed-pass"));
  }

  @Test
  @DisplayName("Unknown id is 404")
  void unknownIdIsNotFound() {
    when(serviceRepository.findById(99)).thenReturn(Optional.empty());
    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setId(99);

    assertThatThrownBy(() -> probeService.probe(request))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("Missing url on create is 400")
  void missingUrlOnCreateIsBadRequest() {
    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setType("WMS");

    assertThatThrownBy(() -> probeService.probe(request))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  @DisplayName("Unsupported type returns failure metadata without calling extractors")
  void unsupportedTypeDoesNotCallExtractor() {
    ServiceCapabilitiesRequest request = new ServiceCapabilitiesRequest();
    request.setUrl("https://example.com/wmts");
    request.setType("WMTS");

    ExtractedMetadata result = probeService.probe(request);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getReason()).isEqualTo("Unsupported service type for capabilities: WMTS");
  }

  private Request capturedRequest() {
    ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
    verify(extractor).extract(captor.capture());
    return captor.getValue();
  }

  private static ExtractedMetadata success() {
    return ExtractedMetadata.builder().success(true).type("OGC:WMS 1.3.0").build();
  }
}
