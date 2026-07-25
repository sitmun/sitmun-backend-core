package org.sitmun.domain.application;

import static org.hamcrest.Matchers.hasItems;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import lombok.extern.java.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.background.ApplicationBackground;
import org.sitmun.domain.application.background.ApplicationBackgroundRepository;
import org.sitmun.domain.application.parameter.ApplicationParameter;
import org.sitmun.domain.application.parameter.ApplicationParameterRepository;
import org.sitmun.domain.application.tree.ApplicationTree;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.background.BackgroundRepository;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.availability.CartographyAvailability;
import org.sitmun.domain.cartography.availability.CartographyAvailabilityRepository;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.permission.CartographyPermissionRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.TreeRepository;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Data REST association PUTs need committed fixtures visible outside the test method transaction.
 * Do not use class {@code @Transactional} or {@code @DirtiesContext} for DB cleanup. Own UUID
 * fixtures; delete them in {@code @AfterEach}; assert membership of owned applications, not
 * absolute seed collection sizes or next ids.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Log
@DisplayName("Application Repository Data REST test")
class ApplicationResourceTest {

  @Autowired ApplicationRepository applicationRepository;
  @Autowired TreeRepository treeRepository;
  @Autowired TerritoryRepository territoryRepository;
  @Autowired TreeNodeRepository treeNodeRepository;
  @Autowired ApplicationBackgroundRepository applicationBackgroundRepository;
  @Autowired BackgroundRepository backgroundRepository;
  @Autowired CartographyPermissionRepository cartographyPermissionRepository;
  @Autowired CartographyRepository cartographyRepository;
  @Autowired ServiceRepository serviceRepository;
  @Autowired CartographyAvailabilityRepository cartographyAvailabilityRepository;
  @Autowired ApplicationParameterRepository applicationParameterRepository;
  @Autowired RoleRepository roleRepository;
  @Autowired UserRepository userRepository;

  @Autowired private MockMvc mvc;

  private Integer backAppId;
  private Integer publicApplicationId;
  private Integer publicSituationMapId;
  private Integer publicRoleId;
  private User eligibleCreator;
  private Set<Tree> trees;
  private Set<Service> services;
  private Set<Cartography> cartographies;
  private Set<CartographyAvailability> cartographyAvailabilities;
  private Set<TreeNode> treeNodes;
  private Set<CartographyPermission> cartographyPermissions;
  private Set<Background> backgrounds;
  private ArrayList<Application> applications;
  private ArrayList<ApplicationParameter> applicationParameters;
  private Territory territory;
  private Role publicRole;
  private String nonPublicApplicationName;
  private String publicApplicationName;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void init() {
    // Keep fixture names short: several STM_* name columns are VARCHAR(30).
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    nonPublicApplicationName = "np-app-" + suffix;
    publicApplicationName = "pub-app-" + suffix;
    String nonPublicApplicationParamName = "np-param-" + suffix;
    String publicApplicationParamName = "pub-param-" + suffix;
    String publicTreeName = "pub-tree-" + suffix;
    String publicBackgroundName = "pub-bg-" + suffix;
    String publicBackgroundMapName = "pub-bmap-" + suffix;
    String publicSituationMapName = "pub-smap-" + suffix;
    String publicCartographyName = "pub-carto-" + suffix;
    String publicTreeNodeName = "pub-node-" + suffix;
    String publicServiceName = "pub-svc-" + suffix;

    territory =
        Territory.builder().name("terr-" + suffix).code("code-" + suffix).blocked(false).build();
    territoryRepository.save(territory);

    applications = new ArrayList<>();
    applicationParameters = new ArrayList<>();

    publicRole = Role.builder().name("pub-role-" + suffix).build();
    roleRepository.save(publicRole);
    publicRoleId = publicRole.getId();

    eligibleCreator =
        userRepository.save(
            User.builder()
                .username("app-poc-" + suffix)
                .password("unused")
                .firstName("PoC")
                .lastName("User")
                .email("poc-" + suffix + "@example.com")
                .administrator(false)
                .blocked(false)
                .build());

    Set<Role> availableRoles = new HashSet<>();
    availableRoles.add(publicRole);

    trees = new HashSet<>();
    Tree publicTree = new Tree();
    publicTree.setName(publicTreeName);
    trees.add(publicTree);
    treeRepository.saveAll(trees);

    publicTree.setAvailableRoles(availableRoles);
    treeRepository.save(publicTree);

    trees = new HashSet<>();
    trees.add(publicTree);

    Service publicService =
        Service.builder()
            .name(publicServiceName)
            .type("some-type")
            .serviceURL("http://some-service-url.com")
            .blocked(false)
            .build();

    services = new HashSet<>();
    services.add(publicService);
    serviceRepository.saveAll(services);

    Cartography publicCartography =
        Cartography.builder()
            .name(publicCartographyName)
            .service(publicService)
            .layers(List.of("Layer1", "Layer2"))
            .queryableFeatureAvailable(false)
            .queryableFeatureEnabled(false)
            .blocked(false)
            .build();

    cartographies = new HashSet<>();
    cartographies.add(publicCartography);
    cartographyRepository.saveAll(cartographies);
    publicCartography = cartographies.iterator().next();

    CartographyAvailability publicCartographyAvailability = new CartographyAvailability();
    publicCartographyAvailability.setCartography(publicCartography);
    publicCartographyAvailability.setTerritory(territory);

    cartographyAvailabilities = new HashSet<>();
    cartographyAvailabilities.add(publicCartographyAvailability);
    cartographyAvailabilityRepository.saveAll(cartographyAvailabilities);

    treeNodes = new HashSet<>();
    TreeNode publicTreeNode = new TreeNode();
    publicTreeNode.setName(publicTreeNodeName);
    publicTreeNode.setCartography(publicCartography);
    publicTreeNode.setTree(publicTree);
    treeNodes.add(publicTreeNode);
    treeNodeRepository.saveAll(treeNodes);

    cartographyPermissions = new HashSet<>();

    CartographyPermission publicBackgroundMap =
        CartographyPermission.builder().name(publicBackgroundMapName).build();
    publicBackgroundMap = cartographyPermissionRepository.save(publicBackgroundMap);

    publicBackgroundMap.getRoles().addAll(availableRoles);
    publicBackgroundMap.getMembers().addAll(cartographies);
    cartographyPermissionRepository.save(publicBackgroundMap);

    cartographyPermissions.add(publicBackgroundMap);

    backgrounds = new HashSet<>();
    Background publicBackground = new Background();
    publicBackground.setName(publicBackgroundName);
    publicBackground.setCartographyGroup(publicBackgroundMap);
    backgrounds.add(publicBackground);
    backgroundRepository.saveAll(backgrounds);
    publicBackground = backgrounds.iterator().next();

    Application application =
        Application.builder().name(nonPublicApplicationName).type("I").jspTemplate("").build();
    SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM d, yyyy HH:mm:ss a");
    try {
      String dateInString = "Friday, Jun 7, 2013 12:10:56 PM";
      application.setCreatedDate(formatter.parse(dateInString));
    } catch (ParseException e) {
      log.warning("Error parsing date:" + e.getMessage());
    }
    applications.add(application);

    CartographyPermission publicSituationMap =
        CartographyPermission.builder()
            .name(publicSituationMapName)
            .type(CartographyPermission.TYPE_SITUATION_MAP)
            .build();
    publicSituationMap = cartographyPermissionRepository.save(publicSituationMap);

    publicSituationMap.getRoles().addAll(availableRoles);
    publicSituationMap.getMembers().addAll(cartographies);
    cartographyPermissionRepository.save(publicSituationMap);

    cartographyPermissions.add(publicSituationMap);
    publicSituationMapId = publicSituationMap.getId();

    Application publicApplication =
        Application.builder()
            .type("I")
            .name(publicApplicationName)
            .situationMap(publicSituationMap)
            .jspTemplate("")
            .createdDate(Date.from(Instant.now()))
            .build();

    applications.add(publicApplication);
    applicationRepository.saveAll(applications);

    publicApplication.getAvailableRoles().addAll(availableRoles);
    publicApplication
        .getTrees()
        .add(ApplicationTree.builder().application(publicApplication).tree(publicTree).build());
    applicationRepository.save(publicApplication);
    publicApplicationId = publicApplication.getId();

    ApplicationBackground publicApplicationBackground = new ApplicationBackground();
    publicApplicationBackground.setBackground(publicBackground);
    publicApplicationBackground.setApplication(publicApplication);
    publicApplicationBackground.setOrder(1);
    Set<ApplicationBackground> applicationBackgrounds = new HashSet<>();
    applicationBackgrounds.add(publicApplicationBackground);
    applicationBackgroundRepository.saveAll(applicationBackgrounds);

    backAppId = publicApplicationBackground.getId();

    ApplicationParameter applicationParam1 = new ApplicationParameter();
    applicationParam1.setName(nonPublicApplicationParamName);
    applicationParam1.setApplication(application);
    applicationParam1.setValue("value");
    applicationParam1.setType("type");
    applicationParameters.add(applicationParam1);

    ApplicationParameter applicationParam2 = new ApplicationParameter();
    applicationParam2.setName(publicApplicationParamName);
    applicationParam2.setApplication(publicApplication);
    applicationParam2.setValue("value");
    applicationParam2.setType("type");
    applicationParameters.add(applicationParam2);

    applicationParameterRepository.saveAll(applicationParameters);
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    if (applicationParameters != null) {
      applicationParameters.forEach(
          item -> {
            if (item.getId() != null) {
              applicationParameterRepository.deleteById(item.getId());
            }
          });
    }
    if (applications != null) {
      applications.forEach(
          item -> {
            if (item.getId() != null) {
              applicationRepository.deleteById(item.getId());
            }
          });
    }
    if (backgrounds != null) {
      backgrounds.forEach(
          item -> {
            if (item.getId() != null) {
              backgroundRepository.deleteById(item.getId());
            }
          });
    }
    if (cartographyPermissions != null) {
      cartographyPermissions.forEach(
          item -> {
            if (item.getId() != null) {
              cartographyPermissionRepository.deleteById(item.getId());
            }
          });
    }
    if (cartographyAvailabilities != null) {
      cartographyAvailabilities.forEach(
          item -> {
            if (item.getId() != null) {
              cartographyAvailabilityRepository.deleteById(item.getId());
            }
          });
    }
    if (treeNodes != null) {
      treeNodes.forEach(
          item -> {
            if (item.getId() != null) {
              treeNodeRepository.deleteById(item.getId());
            }
          });
    }
    if (trees != null) {
      trees.forEach(
          item -> {
            if (item.getId() != null) {
              treeRepository.deleteById(item.getId());
            }
          });
    }
    if (cartographies != null) {
      cartographies.forEach(
          item -> {
            if (item.getId() != null) {
              cartographyRepository.deleteById(item.getId());
            }
          });
    }
    if (services != null) {
      services.forEach(
          item -> {
            if (item.getId() != null) {
              serviceRepository.deleteById(item.getId());
            }
          });
    }
    if (territory != null && territory.getId() != null) {
      territoryRepository.deleteById(territory.getId());
    }
    if (publicRole != null && publicRole.getId() != null) {
      roleRepository.deleteById(publicRole.getId());
    }
    if (eligibleCreator != null && eligibleCreator.getId() != null) {
      userRepository.deleteById(eligibleCreator.getId());
    }
  }

  @Test
  @DisplayName("GET: information about the backgrounds of an application")
  @WithMockUser(roles = "ADMIN")
  void getInformationAboutBackgrounds() throws Exception {
    mvc.perform(get(APPLICATION_BACKGROUNDS_URI + '/' + backAppId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order").value(1));
  }

  @Test
  @DisplayName("PUT: situationMap association succeeds when application has trees")
  @WithMockUser(roles = "ADMIN")
  void putSituationMapAssociationWhenApplicationHasTrees() throws Exception {
    String content = CARTOGRAPHY_PERMISSION_URI.replace("{0}", publicSituationMapId.toString());
    mvc.perform(
            put(APPLICATION_URI_SITUATION_MAP, publicApplicationId)
                .content(content)
                .contentType("text/uri-list"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("PUT: creator association succeeds when application has trees")
  @WithMockUser(roles = "ADMIN")
  void putCreatorAssociationWhenApplicationHasTrees() throws Exception {
    String content = USER_ITEM_URI.replace("{0}", eligibleCreator.getId().toString());
    mvc.perform(
            put(APPLICATION_URI_CREATOR, publicApplicationId)
                .content(content)
                .contentType("text/uri-list"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("PUT: availableRoles association succeeds when application has trees")
  @WithMockUser(roles = "ADMIN")
  void putAvailableRolesAssociationWhenApplicationHasTrees() throws Exception {
    String content = ROLE_URI.replace("{0}", publicRoleId.toString());
    mvc.perform(
            put(APPLICATION_URI_AVAILABLE_ROLES, publicApplicationId)
                .content(content)
                .contentType("text/uri-list"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("GET: information about the layers of an application")
  @WithMockUser(roles = "ADMIN")
  void getServiceLayersAsPublic() throws Exception {
    // ok is expected
    mvc.perform(get(SERVICE_LAYERS_URI, 1)).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET: information as administrator")
  @WithMockUser(roles = "ADMIN")
  void getApplicationsAsSitmunAdmin() throws Exception {
    mvc.perform(get(APPLICATIONS_URI + "?size=100"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                "$._embedded.applications[*].name",
                hasItems(nonPublicApplicationName, publicApplicationName)));
  }
}
