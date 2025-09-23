package org.sitmun.authorization.client.controller;

import static org.sitmun.authorization.client.service.ProfileContext.NodeSectionBehaviour.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.mapstruct.factory.Mappers;
import org.sitmun.authorization.client.dto.*;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.authorization.client.service.ProfileContext;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryDTO;
import org.sitmun.domain.user.position.UserPositionDTO;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/** REST controller for managing client configurations. */
@RestController
@RequestMapping("/api/config/client")
@Slf4j
public class ClientConfigurationController {

  private final AuthorizationService authorizationService;
  private final ProfileMapper profileMapper;
  private final UserPositionRepository userPositionRepository;

  @Value("${sitmun.proxy-middleware.force:false}")
  private boolean proxyForce;

  @Value("${sitmun.proxy-middleware.url:}")
  private String proxyUrl;

  @Value("${sitmun.backend.url:}")
  private String backendUrl;

  @Value("${sitmun.mbtiles.url:}")
  private String mbtilesUrl;

  /**
   * Constructor for ClientConfigurationController.
   *
   * @param authorizationService the authorization service
   * @param profileMapper the profile mapper
   */
  public ClientConfigurationController(
      AuthorizationService authorizationService,
      ProfileMapper profileMapper,
      UserPositionRepository userPositionRepository) {
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
  public PagedModel<TerritoryDTO> getApplicationTerritories(
      @CurrentSecurityContext SecurityContext context,
      @PathVariable Integer appId,
      Pageable pageable) {
    String username = context.getAuthentication().getName();
    if (!authorizationService.mayAccessUser(appId, username)) {
      throw new AccessDeniedException("Access denied to application");
    }
    pageable = ensureSortBy(pageable, "name");
    Page<Territory> page =
        authorizationService.findTerritoriesByUserAndApplication(username, appId, pageable);
    List<TerritoryDTO> territories =
        Mappers.getMapper(TerritoryMapper.class).map(page.getContent());
    return new PagedModel<>(
        new PageImpl<>(territories, page.getPageable(), page.getTotalElements()));
  }

  /** Update a UserPosition from the user */
  @PostMapping(path = "/territory/position", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<UserPositionDTO> editTerritoryPositions(
      @RequestBody UserPositionDTO positionDTOs) {
    userPositionRepository.updatePosition(positionDTOs.getId(), positionDTOs);
    return ResponseEntity.ok(positionDTOs);
  }

  /**
   * Get the list of applications for the current user.
   *
   * @param context the security context
   * @param pageable the pagination information
   * @return a page of applications
   */
  @GetMapping(path = "/application", produces = APPLICATION_JSON_VALUE)
  public PagedModel<ApplicationDtoLittle> getApplications(
      @CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    pageable = ensureSortBy(pageable, "title");
    Page<Application> page = authorizationService.findApplicationsByUser(username, pageable);
    List<ApplicationDtoLittle> applications =
        Mappers.getMapper(ApplicationMapper.class).map(page.getContent());
    decorateApplicationWithMbtiles(applications);
    return new PagedModel<>(
        new PageImpl<>(applications, page.getPageable(), page.getTotalElements()));
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
  public PagedModel<ApplicationDtoLittle> getTerritoryApplications(
      @CurrentSecurityContext SecurityContext context,
      @PathVariable Integer terrId,
      Pageable pageable) {
    String username = context.getAuthentication().getName();
    pageable = ensureSortBy(pageable, "title");
    Page<Application> page =
        authorizationService.findApplicationsByUserAndTerritory(username, terrId, pageable);
    List<ApplicationDtoLittle> applications =
        Mappers.getMapper(ApplicationMapper.class).map(page.getContent());
    return new PagedModel<>(
        new PageImpl<>(applications, page.getPageable(), page.getTotalElements()));
  }

  /**
   * Get the list of territories for the current user.
   *
   * @param context the security context
   * @param pageable the pagination information
   * @return a page of territories
   */
  @GetMapping(path = "/territory", produces = APPLICATION_JSON_VALUE)
  public PagedModel<TerritoryDTO> getTerritories(
      @CurrentSecurityContext SecurityContext context, Pageable pageable) {
    String username = context.getAuthentication().getName();
    pageable = ensureSortBy(pageable, "name");
    Page<Territory> page = authorizationService.findTerritoriesByUser(username, pageable);
    List<TerritoryDTO> territories =
        Mappers.getMapper(TerritoryMapper.class).map(page.getContent());
    return new PagedModel<>(
        new PageImpl<>(territories, page.getPageable(), page.getTotalElements()));
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
  public ResponseEntity<ProfileDto> getProfile(
      @CurrentSecurityContext SecurityContext context,
      @PathVariable("appId") Integer appId,
      @PathVariable("terrId") Integer terrId,
      @RequestParam(value = "filter", defaultValue = "none") String filter) {
    String username = context.getAuthentication().getName();
    if (!authorizationService.mayAccessUser(appId, username)) {
      throw new AccessDeniedException("Access denied to application");
    }

    AtomicReference<ProfileContext.NodeSectionBehaviour> nodeSectionBehaviour =
        new AtomicReference<>(VIRTUAL_ROOT_ALL_NODES);
    AtomicReference<Integer> baseNode = new AtomicReference<>();
    if (Objects.equals(filter, "node")) {
      nodeSectionBehaviour.set(VIRTUAL_ROOT_NODE_PAGE);
    } else {
      extractNode(filter)
          .ifPresent(
              node -> {
                nodeSectionBehaviour.set(ANY_NODE_PAGE);
                baseNode.set(node);
              });
    }

    log.info(
        "Creating profile for appId:{} terrId:{} nodeSectionBehaviour:{} baseNode:{}",
        appId,
        terrId,
        nodeSectionBehaviour.get(),
        baseNode.get());

    ProfileContext profileContext =
        ProfileContext.builder()
            .username(username)
            .appId(appId)
            .territoryId(terrId)
            .nodeId(baseNode.get())
            .nodeSectionBehaviour(nodeSectionBehaviour.get())
            .build();

    return authorizationService
        .createProfile(profileContext)
        .map(it -> profileMapper.map(it, it.getApplication(), it.getTerritory()))
        .map(decorateWithFilter(profileContext))
        .map(decorateWithProxy(profileContext))
        .map(profile -> ResponseEntity.ok().body(profile))
        .orElseGet(() -> ResponseEntity.status(UNAUTHORIZED).build());
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
        profileDto
            .getTrees()
            .forEach(
                tree -> {
                  String rootNode = tree.getRootNode();
                  tree.getNodes()
                      .forEach((nodeId, node) -> decorateNode(context, nodeId, node, rootNode));
                });
      }
      return profileDto;
    };
  }

  private void decorateNode(ProfileContext context, String nodeId, NodeDto node, String rootNode) {
    if (!Objects.equals(nodeId, rootNode)) {
      String uriTemplate =
          backendUrl + "/api/config/client/profile/{appId}/{terrId}?filter={nodeId}";
      String uri =
          UriComponentsBuilder.fromUriString(uriTemplate)
              .build(context.getAppId(), context.getTerritoryId(), nodeId)
              .toString();
      node.setUri(uri);
    }
  }

  /** Decorate the profile with proxy information if necessary. */
  private Function<ProfileDto, ProfileDto> decorateWithProxy(ProfileContext context) {
    return profileDto -> {
      profileDto
          .getServices()
          .forEach(
              service -> {
                if (proxyForce || Boolean.TRUE.equals(service.getIsProxied())) {
                  service.setIsProxied(true);
                  String uriTemplate = proxyUrl + "/proxy/{appId}/{terId}/{type}/{typeId}";
                  log.info(
                      "Creating proxy URL for appId:{} terId:{} type:{} typeId:{} with template:{}",
                      context.getAppId(),
                      context.getTerritoryId(),
                      service.getType(),
                      service.getId().substring(8),
                      uriTemplate);
                  String uri =
                      UriComponentsBuilder.fromUriString(uriTemplate)
                          .build(
                              context.getAppId(),
                              context.getTerritoryId(),
                              service.getType(),
                              service.getId().substring(8))
                          .toString();
                  service.setUrl(uri);
                }
              });
      return profileDto;
    };
  }

  private void decorateApplicationWithMbtiles(List<ApplicationDtoLittle> applications) {
    applications.forEach(app -> {
      if (app.getConfig() == null) {
        app.setConfig(new java.util.HashMap<>());
        app.getConfig().put("mbtilesUrl", mbtilesUrl);
      } else if (!app.getConfig().containsKey("mbtilesUrl")) {
        app.getConfig().put("mbtilesUrl", mbtilesUrl);
      }
    });
  }

  private static @NotNull Pageable ensureSortBy(Pageable pageable, String title) {
    if (pageable.getSort().isUnsorted()) {
      pageable =
          PageRequest.of(
              pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.asc(title)));
    }
    return pageable;
  }
}
