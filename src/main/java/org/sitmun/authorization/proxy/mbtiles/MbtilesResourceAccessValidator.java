package org.sitmun.authorization.proxy.mbtiles;

import static org.sitmun.infrastructure.security.core.SecurityConstants.isPublicPrincipal;
import static org.sitmun.infrastructure.security.jwt.MobileTokenScopes.forMbtilesAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.authorization.client.service.MobileEditionAccessService;
import org.sitmun.authorization.client.service.Profile;
import org.sitmun.authorization.client.service.ProfileContext;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class MbtilesResourceAccessValidator {

  public sealed interface Outcome {
    record Allowed(MbtilesProxyConfigResponseDto response) implements Outcome {}

    record Denied(String detail) implements Outcome {}
  }

  private final AuthorizationService authorizationService;

  public MbtilesResourceAccessValidator(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Transactional(readOnly = true)
  public Outcome authorize(
      MbtilesProxyRequestDto request, String username, long expirationTimeMillis) {
    if (!StringUtils.hasText(username) || isPublicPrincipal(username)) {
      return new Outcome.Denied("Public principal cannot access MBTiles configuration");
    }
    if (forMbtilesAction(request.action()) == null) {
      return new Outcome.Denied("Unsupported MBTiles action");
    }

    Optional<Application> application =
        authorizationService.findApplicationByUserApplicationAndTerritory(
            username, request.appId(), request.territoryId());
    if (application.isEmpty()) {
      return new Outcome.Denied("Application or territory is not accessible");
    }
    if (!MobileEditionAccessService.isEditionApplication(application.get())) {
      return new Outcome.Denied("Application is not an edition application");
    }

    ProfileContext profileContext =
        ProfileContext.builder()
            .username(username)
            .appId(request.appId())
            .territoryId(request.territoryId())
            .nodeSectionBehaviour(ProfileContext.NodeSectionBehaviour.VIRTUAL_ROOT_ALL_NODES)
            .build();

    Optional<Profile> profile = authorizationService.createProfile(profileContext);
    if (profile.isEmpty()) {
      return new Outcome.Denied("Authorized profile is unavailable");
    }

    try {
      CanonicalTileRequestDto tileRequest =
          requiresTileRequest(request.action())
              ? buildCanonicalTileRequest(request, profile.get())
              : null;
      return new Outcome.Allowed(
          new MbtilesProxyConfigResponseDto(
              request.appId(),
              request.territoryId(),
              request.action(),
              username,
              expirationTimeMillis > 0 ? expirationTimeMillis / 1000 : 0,
              tileRequest));
    } catch (IllegalArgumentException ex) {
      return new Outcome.Denied(ex.getMessage());
    }
  }

  private static boolean requiresTileRequest(String action) {
    return "estimate".equalsIgnoreCase(action) || "create".equalsIgnoreCase(action);
  }

  private CanonicalTileRequestDto buildCanonicalTileRequest(
      MbtilesProxyRequestDto request, Profile profile) {
    if (request.services() == null || request.services().isEmpty()) {
      throw new IllegalArgumentException("At least one service reference is required");
    }
    if (request.bbox() == null
        || request.minZoom() == null
        || request.maxZoom() == null
        || !StringUtils.hasText(request.srs())) {
      throw new IllegalArgumentException("bbox, zoom levels and srs are required");
    }
    if (request.minZoom() > request.maxZoom()) {
      throw new IllegalArgumentException("minZoom must be less than or equal to maxZoom");
    }
    if (request.srs().contains("/")
        || request.srs().contains("\\")
        || request.srs().contains("..")) {
      throw new IllegalArgumentException("Invalid SRS value");
    }

    Map<Integer, Service> servicesById =
        profile.getServices().stream()
            .filter(Objects::nonNull)
            .filter(service -> service.getId() != null)
            .collect(Collectors.toMap(Service::getId, Function.identity(), (a, b) -> a));

    Map<Integer, Cartography> layersById =
        profile.getLayers().stream()
            .filter(Objects::nonNull)
            .filter(layer -> layer.getId() != null)
            .collect(Collectors.toMap(Cartography::getId, Function.identity(), (a, b) -> a));

    List<CanonicalTileRequestDto.CanonicalMapServiceDto> mapServices = new ArrayList<>();
    for (MbtilesServiceRefDto ref : request.services()) {
      Service service = servicesById.get(ref.serviceId());
      if (service == null || !StringUtils.hasText(service.getServiceURL())) {
        throw new IllegalArgumentException("Unauthorized or unknown service reference");
      }
      if (service.getServiceURL().contains("..")) {
        throw new IllegalArgumentException("Unauthorized or unknown service reference");
      }

      Set<String> layerNames = new HashSet<>();
      for (Integer layerId : ref.layerIds()) {
        Cartography cartography = layersById.get(layerId);
        if (cartography == null
            || cartography.getService() == null
            || !Objects.equals(cartography.getService().getId(), service.getId())
            || cartography.getLayers() == null
            || cartography.getLayers().isEmpty()) {
          throw new IllegalArgumentException("Unauthorized or unknown layer reference");
        }
        layerNames.addAll(cartography.getLayers());
      }
      if (layerNames.isEmpty()) {
        throw new IllegalArgumentException("Unauthorized or unknown layer reference");
      }

      mapServices.add(
          new CanonicalTileRequestDto.CanonicalMapServiceDto(
              service.getServiceURL(), List.copyOf(layerNames), service.getType()));
    }

    return new CanonicalTileRequestDto(
        mapServices,
        new CanonicalTileRequestDto.CanonicalBboxDto(
            request.bbox().minX(),
            request.bbox().minY(),
            request.bbox().maxX(),
            request.bbox().maxY(),
            request.srs()),
        request.minZoom(),
        request.maxZoom());
  }
}
