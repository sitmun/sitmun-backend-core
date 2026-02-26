package org.sitmun.authorization.proxy.validator;

import static org.sitmun.domain.DomainConstants.Services.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Validates user access to services through the proxy middleware.
 *
 * <p>This validator enforces comprehensive authorization for service requests:
 *
 * <h3>Service-Level Authorization</h3>
 *
 * <ul>
 *   <li>Service must exist and not be blocked
 *   <li>User must have at least one role for the territory
 *   <li>User must have access to at least one cartography using this service
 *   <li>Uses {@code cartographyRepository.findByRolesAndTerritory(roles, territoryId)} to determine
 *       accessible cartographies based on roles and territory
 * </ul>
 *
 * <h3>Layer-Level Authorization (WMS)</h3>
 *
 * <ul>
 *   <li>When LAYERS parameter is present, validates exact match against cartography layer lists
 *   <li>Requested layers must match <strong>exactly</strong> the complete {@code layers} list from
 *       one accessible cartography (no subsets, no mixing from multiple cartographies)
 *   <li>Example: If cartography L1 has layers=[id1,id2] and L2 has layers=[id3], valid requests are
 *       "id1,id2" or "id3", but NOT "id1" or "id1,id3"
 *   <li>Uses the same permission logic as {@link
 *       org.sitmun.authorization.client.service.AuthorizationService} to ensure consistency with
 *       client configuration
 * </ul>
 *
 * <p>The authorization model follows this hierarchy:
 *
 * <pre>
 * User → UserConfiguration (Territory + Role)
 *   → Role → CartographyPermission
 *     → Cartography (members)
 *       → Service + layers[]
 *       → CartographyAvailability (Territory)
 * </pre>
 *
 * <p>This validator is invoked when {@code sitmun.proxy-middleware.validate-user-access=true}.
 *
 * @see org.sitmun.authorization.client.service.AuthorizationService
 * @see org.sitmun.domain.cartography.CartographyRepository#findByRolesAndTerritory
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceResourceAccessValidator implements ResourceAccessValidator {

  // Service-based proxy types (OGC services)
  // Note: API type for tasks is handled by TaskResourceAccessValidator
  private static final Set<String> SUPPORTED_TYPES = Set.of(TYPE_WMS, TYPE_WMTS, TYPE_WFS);

  private final ServiceRepository serviceRepository;
  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final TerritoryRepository territoryRepository;
  private final CartographyRepository cartographyRepository;

  @Override
  public boolean supports(String type) {
    return SUPPORTED_TYPES.stream().anyMatch(t -> t.equalsIgnoreCase(type));
  }

  @Override
  public boolean validate(ConfigProxyRequestDto request, String userName) {
    log.debug(
        "Validating Service access: user={}, appId={}, terId={}, type={}, serviceId={}",
        userName,
        request.getAppId(),
        request.getTerId(),
        request.getType(),
        request.getTypeId());

    // Fetch service and check if blocked
    var serviceOpt = serviceRepository.findById(request.getTypeId());
    if (serviceOpt.isEmpty()) {
      log.warn("Service not found: {}", request.getTypeId());
      return false;
    }

    var service = serviceOpt.get();
    if (Boolean.TRUE.equals(service.getBlocked())) {
      log.warn(
          "Access denied: Service is blocked - serviceId={}, user={}",
          request.getTypeId(),
          userName);
      return false;
    }

    // Check if user exists
    var userOpt = userRepository.findByUsername(userName);
    if (userOpt.isEmpty()) {
      log.warn("User not found: {}", userName);
      return false;
    }

    // Check if application exists
    var applicationExists = applicationRepository.existsById(request.getAppId());
    if (!applicationExists) {
      log.warn("Application not found: {}", request.getAppId());
      return false;
    }

    // Check if territory exists (terId can be 0 for public access)
    if (request.getTerId() > 0) {
      var territoryExists = territoryRepository.existsById(request.getTerId());
      if (!territoryExists) {
        log.warn("Territory not found: {}", request.getTerId());
        return false;
      }
    }

    //  Role-based access control: Check if user has roles that grant access to this service
    // Services are accessed through cartographies which have permissions (groups) with roles
    var user = userOpt.get();
    var userRoles =
        user.getPermissions().stream()
            .filter(
                config ->
                    config.getTerritory() != null
                        && config.getTerritory().getId().equals(request.getTerId()))
            .map(config -> config.getRole())
            .toList();

    if (userRoles.isEmpty()) {
      log.warn(
          "Access denied: User {} has no roles for territory {} - serviceId={}",
          userName,
          request.getTerId(),
          request.getTypeId());
      return false;
    }

    // Get all accessible cartographies for this user/territory
    var accessibleCartographies =
        cartographyRepository.findByRolesAndTerritory(userRoles, request.getTerId());

    // Filter to cartographies for this specific service
    var serviceCartographies =
        accessibleCartographies.stream()
            .filter(
                cartography ->
                    cartography.getService() != null
                        && cartography.getService().getId().equals(service.getId()))
            .toList();

    if (serviceCartographies.isEmpty()) {
      log.warn(
          "Access denied: User {} has no cartography permissions for service {} in territory {}",
          userName,
          request.getTypeId(),
          request.getTerId());
      return false;
    }

    // WMS layer-level authorization
    // Validate that requested layers exactly match one accessible cartography's layer list
    // WMS parameter names are case-insensitive, so lookup LAYERS key ignoring case
    String layersParam = null;
    if (request.getParameters() != null) {
      layersParam =
          request.getParameters().entrySet().stream()
              .filter(e -> "LAYERS".equalsIgnoreCase(e.getKey()))
              .map(Map.Entry::getValue)
              .findFirst()
              .orElse(null);
    }

    if (layersParam != null && !layersParam.isBlank()) {
      Set<String> requestedLayerSet =
          Arrays.stream(layersParam.split(","))
              .map(String::trim)
              .filter(layer -> !layer.isEmpty())
              .collect(Collectors.toSet());

      log.debug(
          "Validating layer access for user {}: requested={}, serviceId={}",
          userName,
          requestedLayerSet,
          service.getId());

      // Check if requested layers exactly match any accessible cartography's layers
      // No subsets, no mixing layers from different cartographies
      boolean matchesAnyCartography =
          serviceCartographies.stream()
              .filter(
                  cartography ->
                      cartography.getLayers() != null && !cartography.getLayers().isEmpty())
              .anyMatch(
                  cartography -> {
                    Set<String> cartographyLayers =
                        cartography.getLayers().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .collect(Collectors.toSet());
                    return requestedLayerSet.equals(cartographyLayers);
                  });

      if (!matchesAnyCartography) {
        List<Set<String>> availableLayerSets =
            serviceCartographies.stream()
                .filter(cartography -> cartography.getLayers() != null)
                .map(
                    cartography ->
                        cartography.getLayers().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .collect(Collectors.toSet()))
                .toList();

        log.warn(
            "Access denied: User {} requested layers {} do not match any accessible cartography for service {} in territory {}. Available layer sets: {}",
            userName,
            requestedLayerSet,
            request.getTypeId(),
            request.getTerId(),
            availableLayerSets);
        return false;
      }

      log.debug(
          "Layer validation passed: requested layers match an accessible cartography for user {}",
          userName);
    }

    log.debug("Service access validation passed for user: {}", userName);
    return true;
  }
}
