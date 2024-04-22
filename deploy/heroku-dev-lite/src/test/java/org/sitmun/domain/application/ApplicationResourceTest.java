package org.sitmun.domain.application;

import lombok.extern.java.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.background.ApplicationBackground;
import org.sitmun.domain.application.background.ApplicationBackgroundRepository;
import org.sitmun.domain.application.parameter.ApplicationParameter;
import org.sitmun.domain.application.parameter.ApplicationParameterRepository;
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
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Log
@DisplayName("Application Repository Data REST test")
class ApplicationResourceTest {

  private static final String NON_PUBLIC_APPLICATION_NAME = "Non-public Application";
  private static final String PUBLIC_APPLICATION_NAME = "Public Application";
  private static final String NON_PUBLIC_APPLICATION_PARAM_NAME = "Non-public Application Param";
  private static final String PUBLIC_APPLICATION_PARAM_NAME = "Public Application Param";
  private static final String PUBLIC_TREE_NAME = "Public tree";
  private static final String PUBLIC_BACKGROUND_NAME = "Public Background Name";
  private static final String PUBLIC_BACKGROUND_MAP_NAME = "Public Background Map Name";
  private static final String PUBLIC_SITUATION_MAP_NAME = "Public Situation Map Name";
  private static final String PUBLIC_CARTOGRAPHY_NAME = "Public Cartography Name";
  private static final String PUBLIC_TREE_NODE_NAME = "Tree Node Name";
  private static final String PUBLIC_SERVICE_NAME = "Public Service Name";
  @Autowired
  ApplicationRepository applicationRepository;
  @Autowired
  TreeRepository treeRepository;
  @Autowired
  TerritoryRepository territoryRepository;
  @Autowired
  TreeNodeRepository treeNodeRepository;
  @Autowired
  ApplicationBackgroundRepository applicationBackgroundRepository;
  @Autowired
  BackgroundRepository backgroundRepository;
  @Autowired
  CartographyPermissionRepository cartographyPermissionRepository;
  @Autowired
  CartographyRepository cartographyRepository;
  @Autowired
  ServiceRepository serviceRepository;
  @Autowired
  CartographyAvailabilityRepository cartographyAvailabilityRepository;
  @Autowired
  ApplicationParameterRepository applicationParameterRepository;
  @Autowired
  RoleRepository roleRepository;

  @Autowired
  private MockMvc mvc;

  private Integer backAppId;
  private ArrayList<Tree> trees;
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

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  public void init() {

      territory = Territory.builder()
        .name("Territorio 1")
        .code("")
        .blocked(false)
        .build();
      territoryRepository.save(territory);

      applications = new ArrayList<>();
      applicationParameters = new ArrayList<>();

      publicRole = Role.builder().name("USUARIO_PUBLICO").build();
      roleRepository.save(publicRole);

      Set<Role> availableRoles = new HashSet<>();
      availableRoles.add(publicRole);

      //Trees
      trees = new ArrayList<>();
      Tree publicTree = new Tree();
      publicTree.setName(PUBLIC_TREE_NAME);
      trees.add(publicTree);
      treeRepository.saveAll(trees);

      publicTree.setAvailableRoles(availableRoles);
      treeRepository.save(publicTree);

      Set<Tree> trees = new HashSet<>();
      trees.add(publicTree);

      //Services
      Service publicService = Service.builder()
        .name(PUBLIC_SERVICE_NAME)
        .type("")
        .serviceURL("")
        .blocked(false)
        .build();
      //publicService.setLayers(cartographies);

      services = new HashSet<>();
      services.add(publicService);
      serviceRepository.saveAll(services);

      //Cartographies
      Cartography publicCartography = Cartography.builder()
        .name(PUBLIC_CARTOGRAPHY_NAME)
        .service(publicService)
        .layers(Collections.emptyList())
        .queryableFeatureAvailable(false)
        .queryableFeatureEnabled(false)
        .blocked(false)
        .build();

      cartographies = new HashSet<>();
      cartographies.add(publicCartography);
      cartographyRepository.saveAll(cartographies);
      publicCartography = cartographies.iterator().next();

      //Cartography availabilities
      CartographyAvailability publicCartographyAvailability = new CartographyAvailability();
      publicCartographyAvailability.setCartography(publicCartography);
      publicCartographyAvailability.setTerritory(territory);

      cartographyAvailabilities = new HashSet<>();
      cartographyAvailabilities.add(publicCartographyAvailability);
      cartographyAvailabilityRepository.saveAll(cartographyAvailabilities);
      // publicCartographyAvailability = cartographyAvailabilities.iterator().next();

      //Tree nodes
      treeNodes = new HashSet<>();
      TreeNode publicTreeNode = new TreeNode();
      publicTreeNode.setName(PUBLIC_TREE_NODE_NAME);
      publicTreeNode.setCartography(publicCartography);
      publicTreeNode.setTree(publicTree);
      treeNodes.add(publicTreeNode);
      treeNodeRepository.saveAll(treeNodes);
      // publicTreeNode = treeNodes.iterator().next();

      //Cartography group
      cartographyPermissions = new HashSet<>();

      CartographyPermission publicBackgroundMap = CartographyPermission.builder()
        .name(PUBLIC_BACKGROUND_MAP_NAME)
        .build();
      publicBackgroundMap = cartographyPermissionRepository.save(publicBackgroundMap);

      publicBackgroundMap.getRoles().addAll(availableRoles);
      publicBackgroundMap.getMembers().addAll(cartographies);
      cartographyPermissionRepository.save(publicBackgroundMap);

      cartographyPermissions.add(publicBackgroundMap);


      //backgrounds
      backgrounds = new HashSet<>();
      Background publicBackground = new Background();
      publicBackground.setName(PUBLIC_BACKGROUND_NAME);
      publicBackground.setCartographyGroup(publicBackgroundMap);
      backgrounds.add(publicBackground);
      backgroundRepository.saveAll(backgrounds);
      publicBackground = backgrounds.iterator().next();

      Application application = Application.builder()
        .name(NON_PUBLIC_APPLICATION_NAME)
        .type("I")
        .jspTemplate("")
        .build();
      SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM d, yyyy HH:mm:ss a");
      try {
        String dateInString = "Friday, Jun 7, 2013 12:10:56 PM";
        application.setCreatedDate(formatter.parse(dateInString));
      } catch (ParseException e) {
        log.warning("Error parsing date:" + e.getMessage());
      }
      applications.add(application);

      CartographyPermission publicSituationMap = CartographyPermission.builder()
        .name(PUBLIC_SITUATION_MAP_NAME)
        .build();
      publicSituationMap = cartographyPermissionRepository.save(publicSituationMap);

      publicSituationMap.getRoles().addAll(availableRoles);
      publicSituationMap.getMembers().addAll(cartographies);
      cartographyPermissionRepository.save(publicSituationMap);

      cartographyPermissions.add(publicSituationMap);

      Application publicApplication = Application.builder()
        .type("I")
        .name(PUBLIC_APPLICATION_NAME)
        .situationMap(publicSituationMap)
        .jspTemplate("")
        .createdDate(Date.from(Instant.now()))
        .build();

      applications.add(publicApplication);
      applicationRepository.saveAll(applications);

      publicApplication.getAvailableRoles().addAll(availableRoles);
      publicApplication.getTrees().addAll(trees);
      applicationRepository.save(publicApplication);

      //application backgrounds
      ApplicationBackground publicApplicationBackground = new ApplicationBackground();
      publicApplicationBackground.setBackground(publicBackground);
      publicApplicationBackground.setApplication(publicApplication);
      publicApplicationBackground.setOrder(1);
      Set<ApplicationBackground> applicationBackgrounds = new HashSet<>();
      applicationBackgrounds.add(publicApplicationBackground);
      applicationBackgroundRepository.saveAll(applicationBackgrounds);

      backAppId = publicApplicationBackground.getId();

      ApplicationParameter applicationParam1 = new ApplicationParameter();
      applicationParam1.setName(NON_PUBLIC_APPLICATION_PARAM_NAME);
      applicationParam1.setApplication(application);
      applicationParam1.setValue("");
      applicationParam1.setType("");
      applicationParameters.add(applicationParam1);

      ApplicationParameter applicationParam2 = new ApplicationParameter();
      applicationParam2.setName(PUBLIC_APPLICATION_PARAM_NAME);
      applicationParam2.setApplication(publicApplication);
      applicationParam2.setValue("");
      applicationParam2.setType("");
      applicationParameters.add(applicationParam2);

      applicationParameterRepository.saveAll(applicationParameters);
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  public void cleanup() {
      applicationParameters
        .forEach((item) -> applicationParameterRepository.deleteById(item.getId()));
      applications.forEach((item) -> applicationRepository.deleteById(item.getId()));
      backgrounds.forEach((item) -> backgroundRepository.deleteById(item.getId()));
      cartographyPermissions
        .forEach((item) -> cartographyPermissionRepository.deleteById(item.getId()));
      cartographyAvailabilities
        .forEach((item) -> cartographyAvailabilityRepository.deleteById(item.getId()));
      treeNodes.forEach((item) -> treeNodeRepository.deleteById(item.getId()));
      trees.forEach((item) -> treeRepository.deleteById(item.getId()));
      cartographies.forEach((item) -> cartographyRepository.deleteById(item.getId()));
      services.forEach((item) -> serviceRepository.deleteById(item.getId()));
      territoryRepository.delete(territory);
      roleRepository.delete(publicRole);
  }

  @Test
  @DisplayName("GET: information about the backgrounds of an application")
  void getInformationAboutBackgrounds() throws Exception {
    mvc.perform(get(URIConstants.APPLICATION_BACKGROUNDS_URI + '/' + backAppId)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.order").value(1));
  }

  @Test
  @DisplayName("GET: information about the layers of an application")
  void getServiceLayersAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(URIConstants.SERVICE_URI + "/1/layers")
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET: information as administrator")
  void getApplicationsAsSitmunAdmin() throws Exception {
    // ok is expected
    mvc.perform(get(URIConstants.APPLICATIONS_URI)
        .with(SecurityMockMvcRequestPostProcessors.user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.applications", hasSize(7)));
  }

}

