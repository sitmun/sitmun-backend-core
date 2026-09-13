package org.sitmun.administration.service.extractor.capabilities;

import java.util.List;
import java.util.Optional;
import okhttp3.Credentials;
import okhttp3.Request;
import org.sitmun.administration.controller.dto.ServiceCapabilitiesRequest;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ServiceCapabilitiesProbeService {

  static final String AUTH_NONE = "None";
  static final String AUTH_HTTP_BASIC = "HTTP Basic authentication";

  private final ServiceRepository serviceRepository;
  private final List<ServiceCapabilitiesExtractor> extractors;

  public ServiceCapabilitiesProbeService(
      ServiceRepository serviceRepository, @NonNull List<ServiceCapabilitiesExtractor> extractors) {
    this.serviceRepository = serviceRepository;
    this.extractors = extractors;
  }

  public ExtractedMetadata probe(ServiceCapabilitiesRequest request) {
    ProbeTarget target = resolve(request);
    String capabilitiesUrl;
    try {
      capabilitiesUrl = CapabilitiesUrlBuilder.build(target.url(), target.type());
    } catch (IllegalArgumentException exception) {
      return ExtractedMetadata.builder().success(false).reason(exception.getMessage()).build();
    }
    Request.Builder httpRequest =
        new Request.Builder().url(capabilitiesUrl).header("Accept", "*/*");
    if (useHttpBasic(target.authenticationMode(), target.password())) {
      String username = target.user() == null ? "" : target.user();
      httpRequest.header("Authorization", Credentials.basic(username, target.password()));
    }
    Request built = httpRequest.build();
    ExtractedMetadata capabilities =
        ExtractedMetadata.builder().success(false).reason("No available extractor").build();
    for (ServiceCapabilitiesExtractor extractor : extractors) {
      capabilities = extractor.extract(built);
      if (Boolean.TRUE.equals(capabilities.getSuccess())) {
        return capabilities;
      }
    }
    return capabilities;
  }

  private ProbeTarget resolve(ServiceCapabilitiesRequest request) {
    if (request.getId() == null) {
      if (request.getUrl() == null || request.getUrl().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url is required");
      }
      if (request.getType() == null || request.getType().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
      }
      return new ProbeTarget(
          request.getUrl(),
          request.getType(),
          request.getAuthenticationMode(),
          request.getUser(),
          request.isPasswordPresent() ? request.getPassword() : null);
    }
    Optional<Service> stored = serviceRepository.findById(request.getId());
    if (stored.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found");
    }
    Service service = stored.get();
    String url = firstNonBlank(request.getUrl(), service.getServiceURL());
    String type = firstNonBlank(request.getType(), service.getType());
    String mode =
        request.getAuthenticationMode() != null
            ? request.getAuthenticationMode()
            : service.getAuthenticationMode();
    String user = request.getUser() != null ? request.getUser() : service.getUser();
    String password = request.isPasswordPresent() ? request.getPassword() : service.getPassword();
    if (url == null || url.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "url is required");
    }
    if (type == null || type.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
    }
    return new ProbeTarget(url, type, mode, user, password);
  }

  private static boolean useHttpBasic(String authenticationMode, String password) {
    return AUTH_HTTP_BASIC.equals(authenticationMode) && password != null && !password.isBlank();
  }

  private static String firstNonBlank(String overlay, String stored) {
    if (overlay != null && !overlay.isBlank()) {
      return overlay;
    }
    return stored;
  }

  private record ProbeTarget(
      String url, String type, String authenticationMode, String user, String password) {}
}
