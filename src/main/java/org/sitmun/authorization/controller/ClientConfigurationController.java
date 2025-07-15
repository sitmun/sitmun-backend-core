package org.sitmun.authorization.controller;

import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.dto.*;
import org.sitmun.authorization.service.AuthorizationService;
import org.sitmun.authorization.service.ProfileContext;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryDTO;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionDTO;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.sitmun.authorization.service.ProfileContext.NodeSectionBehaviour.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * REST controller for managing client configurations.
 */
@RestController
@RequestMapping("/api/config/client")
@Slf4j
public class ClientConfigurationController {

  private final AuthorizationService authorizationService;
  private final ProfileMapper profileMapper;
  private final UserPositionRepository userPositionRepository;

  @Value("${sitmun.proxy.force:false}")
  private boolean proxyForce;
  @Value("${sitmun.proxy.url:}")
  private String proxyUrl;
  @Value("${sitmun.backend.url:}")
  private String backendUrl;

  /**
   * Constructor for ClientConfigurationController.
   *
   * @param authorizationService the authorization service
   * @param profileMapper the profile mapper
   */
  public ClientConfigurationController(AuthorizationService authorizationService, ProfileMapper profileMapper, UserPositionRepository userPositionRepository) {
    this.authorizationService = authorizationService;
    this.profileMapper = profileMapper;
    this.userPositionRepository = userPositionRepository;
  }

  /**
   * Get the territories for a specific application.
   *
   * @param context the security context
   * @param appId the application ID
   * @param pageable the pagination information
   * @return a page of territories
   */
  @GetMapping(path = "/application/{appId}/territories", produces = APPLICATION_JSON_VALUE)
  public Page<TerritoryDTO> getApplicationTerritories(@CurrentSecurityContext SecurityContext context, @PathVariable Integer appId, Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("name")));
    }
    Page<Territory> page = authorizationService.findTerritoriesByUserAndApplication(username, appId, pageable);
    List<TerritoryDTO> territories = Mappers.getMapper(TerritoryMapper.class).map(page.getContent());
    return new PageImpl<>(territories, page.getPageable(), page.getTotalElements());
  }

  /**
   * Update a UserPosition from the user
   */
  @PutMapping(path = "/territory/position", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<UserPositionDTO> editTerritoryPositions(@RequestBody UserPositionDTO positionDTOs) {
    userPositionRepository.updatePosition(positionDTOs.getId(), positionDTOs);
    return ResponseEntity.ok(positionDTOs);
  }

  private UserPositionDTO convertUserPositionToDTO(UserPosition userPosition) {
    return UserPositionDTO.builder()
      .id(userPosition.getId())
      .name(userPosition.getName())
      .organization(userPosition.getOrganization())
      .email(userPosition.getEmail())
      .createdDate(userPosition.getCreatedDate())
      .lastModifiedDate(userPosition.getLastModifiedDate())
      .expirationDate(userPosition.getExpirationDate())
      .type(userPosition.getType())
      .userId(userPosition.getUser() != null ? userPosition.getUser().getId() : null)
      .territoryId(userPosition.getTerritory() != null ? userPosition.getTerritory().getId() : null)
      .build();
  }

  /**
   * Get the list of applications for the current user.
   *
   * @param context the security context
   * @param pageable the pagination information
   * @return a page of applications
   */
  @GetMapping(path = "/application", produces = APPLICATION_JSON_VALUE)
  public Page<ApplicationDtoLittle> getApplications(@CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("title")));
    }
    Page<Application> page = authorizationService.findApplicationsByUser(username, pageable);
    List<ApplicationDtoLittle> applications = Mappers.getMapper(ApplicationMapper.class).map(page.getContent());
    return new PageImpl<>(applications, page.getPageable(), page.getTotalElements());
  }

  /**
   * Get the list of applications in a specific territory for the current user.
   *
   * @param context the security context
   * @param terrId the territory ID
   * @param pageable the pagination information
   * @return a page of applications
   */
  @GetMapping(path = "/territory/{terrId}/applications", produces = APPLICATION_JSON_VALUE)
  public Page<ApplicationDtoLittle> getTerritoryApplications(@CurrentSecurityContext SecurityContext context, @PathVariable Integer terrId, Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("title")));
    }
    Page<Application> page = authorizationService.findApplicationsByUserAndTerritory(username, terrId, pageable);
    List<ApplicationDtoLittle> applications = Mappers.getMapper(ApplicationMapper.class).map(page.getContent());
    return new PageImpl<>(applications, page.getPageable(), page.getTotalElements());
  }

  /**
   * Get the list of territories for the current user.
   *
   * @param context the security context
   * @param pageable the pagination information
   * @return a page of territories
   */
  @GetMapping(path = "/territory", produces = APPLICATION_JSON_VALUE)
  public Page<TerritoryDTO> getTerritories(@CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (pageable.getSort().isUnsorted()) {
      pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc("name")));
    }
    Page<Territory> page = authorizationService.findTerritoriesByUser(username, pageable);
    List<TerritoryDTO> territories = Mappers.getMapper(TerritoryMapper.class).map(page.getContent());
    return new PageImpl<>(territories, page.getPageable(), page.getTotalElements());
  }

  /**
   * Get the profile for a specific application and territory.
   *
   * @param context the security context
   * @param appId the application ID
   * @param terrId the territory ID
   * @return the profile
   */
  @GetMapping(path = "/profile/{appId}/{terrId}", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<ProfileDto> getProfile(@CurrentSecurityContext SecurityContext context,
                                               @PathVariable("appId") Integer appId,
                                               @PathVariable("terrId") Integer terrId,
                                               @RequestParam(value = "filter", defaultValue = "none") String filter
  ) {
    AtomicReference<ProfileContext.NodeSectionBehaviour> nodeSectionBehaviour = new AtomicReference<>(VIRTUAL_ROOT_ALL_NODES);
    AtomicReference<Integer> baseNode = new AtomicReference<>();
    if (Objects.equals(filter, "node")) {
      nodeSectionBehaviour.set(VIRTUAL_ROOT_NODE_PAGE);
    } else {
      extractNode(filter).ifPresent(node -> {
        nodeSectionBehaviour.set(ANY_NODE_PAGE);
        baseNode.set(node);
      });
    }

    log.info("Creating profile for appId:{} terrId:{} nodeSectionBehaviour:{} baseNode:{}",
      appId,
      terrId,
      nodeSectionBehaviour.get(),
      baseNode.get());

    ProfileContext profileContext = ProfileContext.builder()
      .username(context.getAuthentication().getName())
      .appId(appId)
      .territoryId(terrId)
      .nodeId(baseNode.get())
      .nodeSectionBehaviour(nodeSectionBehaviour.get())
      .build();

    return authorizationService.createProfile(profileContext)
      .map(it -> profileMapper.map(it, it.getApplication(), it.getTerritory()))
      .map(decorateWithFilter(profileContext))
      .map(decorateWithProxy(profileContext))
      .map(profile -> ResponseEntity.ok().body(profile))
      .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  private static final Pattern NODE_PATTERN = Pattern.compile("node/(\\d+)");

  public static Optional<Integer> extractNode(String input) {
    if (input == null) {
      return Optional.empty();
    }
    Matcher matcher = NODE_PATTERN.matcher(input);
    if (matcher.find()) {
      try {
        return Optional.of(Integer.parseInt(matcher.group(1)));
      } catch (NumberFormatException e) {
        log.error("Error parsing node from filter: {}", input);
        // Log the exception if necessary
      }
    }
    return Optional.empty();
  }

  private Function<ProfileDto, ProfileDto> decorateWithFilter(ProfileContext context) {
    return profileDto -> {
      if (context.getNodeSectionBehaviour().nodePageMode()) {
        profileDto.getTrees().forEach(tree -> {
          String rootNode = tree.getRootNode();
          tree.getNodes().forEach((nodeId, node) -> {
            if (!Objects.equals(nodeId, rootNode)) {
              String uriTemplate = backendUrl + "/api/config/client/profile/{appId}/{terrId}?filter={nodeId}";
              String uri = UriComponentsBuilder.fromUriString(uriTemplate)
                .build(context.getAppId(), context.getTerritoryId(), nodeId)
                .toString();
              node.setUri(uri);
            }
          });
        });
      }
      return profileDto;
    };
  }

  /**
   * Decorate the profile with proxy information if necessary.
   */
  private Function<ProfileDto, ProfileDto> decorateWithProxy(ProfileContext context) {
    return profileDto -> {
      profileDto.getServices().forEach(service -> {
        if (proxyForce || Boolean.TRUE.equals(service.getIsProxied())) {
          service.setIsProxied(true);
          String uriTemplate = proxyUrl + "/proxy/{appId}/{terId}/{type}/{typeId}";
          log.info("Creating proxy URL for appId:{} terId:{} type:{} typeId:{} with template:{}",
            context.getAppId(),
            context.getTerritoryId(),
            service.getType(),
            service.getId().substring(8),
            uriTemplate);
          String uri = UriComponentsBuilder.fromUriString(uriTemplate)
            .build(context.getAppId(), context.getTerritoryId(), service.getType(), service.getId().substring(8))
            .toString();
          service.setUrl(uri);
        }
      });
      return profileDto;
    };
  }
}