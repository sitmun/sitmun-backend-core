package org.sitmun.domain.tree.node;

import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.style.CartographyStyle;
import org.sitmun.domain.cartography.style.CartographyStyleRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.TreeRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class TreeNodeResourceTest {

  private static final String NON_PUBLIC_TREENODE_NAME = "Non-Tree Node";
  private static final String PUBLIC_TREENODE_NAME = "Tree Node";
  private static final String PUBLIC_TREE_NAME = "Tree";
  private static final String NON_PUBLIC_TREE_NAME = "Non-Tree Name";
  @Autowired
  TreeRepository treeRepository;
  @Autowired
  TreeNodeRepository treeNodeRepository;
  @Autowired
  CartographyRepository cartographyRepository;
  @Autowired
  CartographyStyleRepository cartographyStyleRepository;
  @Autowired
  RoleRepository roleRepository;
  @Autowired
  ServiceRepository serviceRepository;
  @Autowired
  private MockMvc mvc;

  private CartographyStyle style;
  private ArrayList<Tree> trees;
  private ArrayList<TreeNode> nodes;

  private Cartography cartography;

  private Set<Role> availableRoles;

  private Service service;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void init() {
      nodes = new ArrayList<>();
    Role publicRole = Role.builder().name("USUARIO_PUBLICO").build();
      roleRepository.save(publicRole);

    availableRoles = new HashSet<>();
      availableRoles.add(publicRole);

      trees = new ArrayList<>();

    Tree publicTree = new Tree();
      publicTree.setName(PUBLIC_TREE_NAME);
    treeRepository.save(publicTree);
      trees.add(publicTree);

    publicTree.getAvailableRoles().addAll(availableRoles);
    treeRepository.save(publicTree);

      Tree tree = new Tree();
      tree.setName(NON_PUBLIC_TREE_NAME);
    treeRepository.save(tree);
      trees.add(tree);

    service = Service.builder()
      .name("Service")
      .serviceURL("")
      .type("")
      .blocked(false)
      .build();
    serviceRepository.save(service);

    cartography = Cartography.builder()
      .type("I")
      .name("Carto")
      .layers(Collections.emptyList())
      .queryableFeatureAvailable(false)
      .queryableFeatureEnabled(false)
      .service(service)
      .availabilities(Collections.emptySet())
      .blocked(false)
      .build();
      cartography = cartographyRepository.save(cartography);

    style = CartographyStyle.builder()
      .name("Style D")
      .cartography(cartography)
      .defaultStyle(true).build();
      cartographyStyleRepository.save(style);

      TreeNode treeNode1 = new TreeNode();
      treeNode1.setName(NON_PUBLIC_TREENODE_NAME);
      treeNode1.setCartography(cartography);
      treeNode1.setTree(tree);
      nodes.add(treeNode1);

      TreeNode treeNode2 = new TreeNode();
      treeNode2.setName(PUBLIC_TREENODE_NAME);
      treeNode2.setTree(publicTree);

      nodes.add(treeNode2);
      treeNodeRepository.saveAll(nodes);
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    treeNodeRepository.deleteAll(nodes);
    cartographyStyleRepository.delete(style);
    cartographyRepository.delete(cartography);
    serviceRepository.delete(service);
    treeRepository.deleteAll(trees);
    roleRepository.deleteAll(availableRoles);
  }

  @DisplayName("Tree nodes cannot be created with non existent styles")
  @Disabled("Requires additional test data")
  @Test
  void nodesCantBeCreatedWithNonExistentStyles() throws Exception {
    TreeNode node = nodes.get(0);
    JSONObject json = new JSONObject();
    json.put("name", node.getName());
    json.put("tree", "/" + node.getTree().getId());
    json.put("cartography", "/" + cartography.getId());
    json.put("style", "Style D");
    mvc.perform(post(URIConstants.TREE_NODES_URI).content(json.toString())
        .with(user(Fixtures.admin())))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Tree node style not found in the tree node cartography's styles"));
  }

  @DisplayName("Tree nodes styles require a cartography")
  @Test
  void nodesStylesRequireCartography() throws Exception {
    TreeNode node = nodes.get(0);
    JSONObject json = new JSONObject();
    json.put("name", node.getName());
    json.put("tree", "/" + node.getTree().getId());
    json.put("style", "Style D");
    mvc.perform(post(URIConstants.TREE_NODES_URI).content(json.toString())
        .with(user(Fixtures.admin())))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Tree node style requires a tree node with cartography"));
  }

  @Test
  @DisplayName("Find nodes based on a list of trees")
  void findNodesBasedOnTreeList() {
    List<Role> roles = roleRepository.findRolesByApplicationAndUserAndTerritory("public", 1, 1);
    List<Tree> tr = treeRepository.findByAppAndRoles(1, roles);
    List<TreeNode> nodes = treeNodeRepository.findByTrees(tr);
    assertThat(nodes).hasSize(9);
  }
}
