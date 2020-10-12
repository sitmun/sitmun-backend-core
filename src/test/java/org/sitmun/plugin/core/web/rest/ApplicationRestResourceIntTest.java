package org.sitmun.plugin.core.web.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.domain.Application;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.sitmun.plugin.core.domain.ApplicationParameter;
import org.sitmun.plugin.core.domain.Background;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.CartographyAvailability;
import org.sitmun.plugin.core.domain.CartographyGroup;
import org.sitmun.plugin.core.domain.Role;
import org.sitmun.plugin.core.domain.Service;
import org.sitmun.plugin.core.domain.Territory;
import org.sitmun.plugin.core.domain.Tree;
import org.sitmun.plugin.core.domain.TreeNode;
import org.sitmun.plugin.core.repository.ApplicationBackgroundRepository;
import org.sitmun.plugin.core.repository.ApplicationParameterRepository;
import org.sitmun.plugin.core.repository.ApplicationRepository;
import org.sitmun.plugin.core.repository.BackgroundRepository;
import org.sitmun.plugin.core.repository.CartographyAvailabilityRepository;
import org.sitmun.plugin.core.repository.CartographyGroupRepository;
import org.sitmun.plugin.core.repository.CartographyRepository;
import org.sitmun.plugin.core.repository.RoleRepository;
import org.sitmun.plugin.core.repository.ServiceRepository;
import org.sitmun.plugin.core.repository.TerritoryRepository;
import org.sitmun.plugin.core.repository.TreeNodeRepository;
import org.sitmun.plugin.core.repository.TreeRepository;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApplicationRestResourceIntTest {

  @TestConfiguration
  static class ContextConfiguration {
    @Bean
    public Validator validator() {
      return new LocalValidatorFactoryBean();
    }

    @Bean
    RepositoryRestConfigurer repositoryRestConfigurer() {
      return new RepositoryRestConfig(validator());
    }
  }

  private static final String ADMIN_USERNAME = "admin";
  private static final String APP_URI = "http://localhost/api/applications";
  private static final String CARTOGRAPHY_GROUP_URI = "http://localhost/api/cartography-groups";
  private static final String NON_PUBLIC_APPLICATION_NAME = "Non-public Application";
  private static final String PUBLIC_APPLICATION_NAME = "Public Application";
  private static final String NON_PUBLIC_APPLICATION_PARAM_NAME = "Non-public Application Param";
  private static final String PUBLIC_APPLICATION_PARAM_NAME = "Public Application Param";
  private static final String PUBLIC_TREE_NAME = "Public tree";
  private static final String PUBLIC_BACKGROUND_NAME = "Public Background Name";
  private static final String PUBLIC_CARTOGRAPHY_GROUP_NAME = "Public Cartography Group Name";
  private static final String PUBLIC_CARTOGRAPHY_NAME = "Public Cartography Name";
  private static final String PUBLIC_TREE_NODE_NAME = "Tree Node Name";
  private static final String TREE_NODE_URI = "http://localhost/api/tree-nodes";
  private static final String APP_BACKGROUNDS_URI = "http://localhost/api/application-backgrounds";
  private static final String PUBLIC_SERVICE_NAME = "Public Service Name";
  private static final String SERVICE_URI = "http://localhost/api/services";
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
  CartographyGroupRepository cartographyGroupRepository;
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
  TokenProvider tokenProvider;
  @Value("${default.territory.name}")
  private String defaultTerritoryName;
  @Autowired
  private MockMvc mvc;

  private BigInteger appId;
  private BigInteger backAppId;

  @Before
  public void init() {
    Territory territory = territoryRepository.findOneByName(defaultTerritoryName).get();
    ArrayList<Application> appsToCreate = new ArrayList<>();
    ArrayList<ApplicationParameter> parametersToCreate = new ArrayList<>();
    Role publicRole = this.roleRepository.findOneByName(AuthoritiesConstants.USUARIO_PUBLICO).get();

    Set<Role> availableRoles = new HashSet<>();
    availableRoles.add(publicRole);

    //Trees
    ArrayList<Tree> treesToCreate = new ArrayList<>();
    Tree publicTree = new Tree();
    publicTree.setName(PUBLIC_TREE_NAME);
    publicTree.setAvailableRoles(availableRoles);
    treesToCreate.add(publicTree);
    this.treeRepository.saveAll(treesToCreate);
    Set<Tree> trees = new HashSet<>();
    trees.add(publicTree);

    //Services
    Service publicService = new Service();
    publicService.setName(PUBLIC_SERVICE_NAME);
    publicService.setType("");
    publicService.setServiceURL("");
    //publicService.setLayers(cartographies);

    Set<Service> services = new HashSet<>();
    services.add(publicService);
    this.serviceRepository.saveAll(services);

    //Cartographies
    Cartography publicCartography = Cartography.builder()
        .setName(PUBLIC_CARTOGRAPHY_NAME)
        .setService(publicService)
        .setLayers(Collections.emptyList())
        .setApplyFilterToGetMap(false)
        .setApplyFilterToSpatialSelection(false)
        .setApplyFilterToGetFeatureInfo(false)
        .build();

    Set<Cartography> cartographies = new HashSet<>();
    cartographies.add(publicCartography);
    this.cartographyRepository.saveAll(cartographies);
    publicCartography = cartographies.iterator().next();

    //Cartography availabilities
    CartographyAvailability publicCartographyAvailability = new CartographyAvailability();
    publicCartographyAvailability.setCartography(publicCartography);
    publicCartographyAvailability.setTerritory(territory);
    Set<CartographyAvailability> cartographyAvailabilities = new HashSet<>();
    cartographyAvailabilities.add(publicCartographyAvailability);
    this.cartographyAvailabilityRepository.saveAll(cartographyAvailabilities);
    // publicCartographyAvailability = cartographyAvailabilities.iterator().next();

    //Tree nodes
    Set<TreeNode> treeNodes = new HashSet<>();
    TreeNode publicTreeNode = new TreeNode();
    publicTreeNode.setName(PUBLIC_TREE_NODE_NAME);
    publicTreeNode.setCartography(publicCartography);
    publicTreeNode.setTree(publicTree);
    treeNodes.add(publicTreeNode);
    this.treeNodeRepository.saveAll(treeNodes);
    // publicTreeNode = treeNodes.iterator().next();

    //Cartography group
    Set<CartographyGroup> cartographyGroups = new HashSet<>();
    CartographyGroup publicCartographyGroup = new CartographyGroup();
    publicCartographyGroup.setName(PUBLIC_CARTOGRAPHY_GROUP_NAME);
    publicCartographyGroup.setRoles(availableRoles);
    publicCartographyGroup.setMembers(cartographies);
    cartographyGroups.add(publicCartographyGroup);
    cartographyGroupRepository.saveAll(cartographyGroups);
    publicCartographyGroup = cartographyGroups.iterator().next();

    //backgrounds
    Set<Background> backgrounds = new HashSet<>();
    Background publicBackground = new Background();
    publicBackground.setName(PUBLIC_BACKGROUND_NAME);
    publicBackground.setCartographyGroup(publicCartographyGroup);
    backgrounds.add(publicBackground);
    backgroundRepository.saveAll(backgrounds);
    publicBackground = backgrounds.iterator().next();


    Application application = new Application();
    application.setName(NON_PUBLIC_APPLICATION_NAME);
    application.setJspTemplate("");
    SimpleDateFormat formatter = new SimpleDateFormat("EEEE, MMM dd, yyyy HH:mm:ss a");
    String dateInString = "Friday, Jun 7, 2013 12:10:56 PM";
    try {
      application.setCreatedDate(formatter.parse(dateInString));
    } catch (Exception e) {
      e.printStackTrace();
    }
    appsToCreate.add(application);

    Application publicApplication = new Application();
    publicApplication.setName(PUBLIC_APPLICATION_NAME);
    publicApplication.setAvailableRoles(availableRoles);
    publicApplication.setTrees(trees);
    publicApplication.setSituationMap(publicCartographyGroup);
    publicApplication.setJspTemplate("");

    appsToCreate.add(publicApplication);
    applicationRepository.saveAll(appsToCreate);

    appId = appsToCreate.get(0).getId();

    //application backgrounds
    Set<ApplicationBackground> applicationBackgrounds = new HashSet<>();
    ApplicationBackground publicApplicationBackground = new ApplicationBackground();
    publicApplicationBackground.setBackground(publicBackground);
    publicApplicationBackground.setApplication(publicApplication);
    publicApplicationBackground.setOrder(BigInteger.ONE);
    applicationBackgrounds.add(publicApplicationBackground);
    applicationBackgroundRepository.saveAll(applicationBackgrounds);

    backAppId = publicApplicationBackground.getId();

    ApplicationParameter applicationParam1 = new ApplicationParameter();
    applicationParam1.setName(NON_PUBLIC_APPLICATION_PARAM_NAME);
    applicationParam1.setApplication(application);
    applicationParam1.setValue("");
    applicationParam1.setType("");
    parametersToCreate.add(applicationParam1);

    ApplicationParameter applicationParam2 = new ApplicationParameter();
    applicationParam2.setName(PUBLIC_APPLICATION_PARAM_NAME);
    applicationParam2.setApplication(publicApplication);
    applicationParam2.setValue("");
    applicationParam2.setType("");
    parametersToCreate.add(applicationParam2);

    applicationParameterRepository.saveAll(parametersToCreate);
    // Create user
    // Create territory
    // Create role
    // Create application
  }

  @Test
  public void getPublicApplicationsAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(APP_URI))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.applications", hasSize(1)));
  }

  @Ignore
  public void getPublicApplicationParamsAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(APP_URI + "/2/parameters"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Ignore
  @WithMockUser(username = ADMIN_USERNAME)
  public void getInformationAboutAnApp() throws Exception {
    mvc.perform(get(APP_URI + "/" + appId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Non-public Application"))
        .andExpect(jsonPath("$.createdDate").value("2013-06-07T10:10:56.000+0000"));
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void getInformationAboutBackgrounds() throws Exception {
    mvc.perform(get(APP_BACKGROUNDS_URI + "/" + backAppId))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.order").value(1));
  }


  @Test
  public void getPublicApplicationTreesAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(APP_URI + "/2/trees"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void getPublicApplicationBackgroundsAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(APP_URI + "/2/backgrounds"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Ignore
  public void getPublicApplicationSituationMapsAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(APP_URI + "/2/situationMap"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void getCartographyGroupMembersAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(CARTOGRAPHY_GROUP_URI + "/1/members"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Ignore
  public void getTreeNodeCartographyAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(TREE_NODE_URI + "/1/cartography"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void getServiceLayersAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(SERVICE_URI + "/1/layers"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void getApplicationsAsTerritorialUser() {
    // TODO
    // ok is expected
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void getApplicationsAsSitumunAdmin() throws Exception {
    // ok is expected
    mvc.perform(get(APP_URI))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.applications", hasSize(2)));
  }

  @Test
  public void getApplicationsAsOrganizationAdmin() {
    // TODO
    // ok is expected
  }

  @Test
  public void setAvailableRolesAsPublicFails() {
    // TODO
    // fail is expected
  }

  @Test
  public void setAvailableRolesAsTerritorialUserFails() {
    // TODO
    // fail is expected
  }

  @Test
  public void setAvailableRolesAsSitmunAdmin() {
    // TODO Update available roles for the app as an admin user
    // ok is expected
  }

  @Test
  public void setTreeAsSitmunAdmin() {
    // TODO Update tree for the app as an admin user
    // ok is expected
  }

  @Test
  public void setBackgroundAsSitmunAdmin() {
    // TODO:Update background for the app as an admin user
    // ok is expected
  }

  @Test
  public void setAvailableRolesAsOrganizationAdmin() {
    // TODO: Update available roles for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Test
  public void setTreeAsOrganizationAdmin() {
    // TODO: Update tree for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Test
  public void setBackgroundAsOrganizationAdmin() {
    // TODO: Update background for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Test
  public void setAvailableRolesAsOtherOrganizationAdminFails() {
    // TODO: Update available roles for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

  @Test
  public void setTreeAsOtherOrganizationAdminFails() {
    // TODO: Update tree for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

  @Test
  public void setBackgroundAsOtherOrganizationAdminFails() {
    // TODO: Update background for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

}
