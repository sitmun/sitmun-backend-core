package org.sitmun.domain.tree.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isolation follows Spring TestContext guidance: keep the ApplicationContext cached, wrap each test
 * in a transaction that rolls back, and do not use {@code @DirtiesContext} for DB cleanup. Fixtures
 * are owned by this class; assertions never depend on Liquibase seed rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TreeNodeResourceTest {

  @Autowired TreeRepository treeRepository;
  @Autowired TreeNodeRepository treeNodeRepository;
  @Autowired CartographyRepository cartographyRepository;
  @Autowired CartographyStyleRepository cartographyStyleRepository;
  @Autowired RoleRepository roleRepository;
  @Autowired ServiceRepository serviceRepository;
  @Autowired private MockMvc mvc;

  private List<Tree> trees;
  private List<TreeNode> nodes;
  private Cartography cartography;

  @BeforeEach
  void init() {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    nodes = new ArrayList<>();
    trees = new ArrayList<>();

    Role publicRole = Role.builder().name("USUARIO_PUBLICO_TEST-" + suffix).build();
    roleRepository.save(publicRole);
    Set<Role> availableRoles = new HashSet<>();
    availableRoles.add(publicRole);

    Tree publicTree = new Tree();
    publicTree.setName("Tree-" + suffix);
    treeRepository.save(publicTree);
    trees.add(publicTree);
    publicTree.getAvailableRoles().addAll(availableRoles);
    treeRepository.save(publicTree);

    Tree tree = new Tree();
    tree.setName("Non-Tree-" + suffix);
    treeRepository.save(tree);
    trees.add(tree);

    Service service =
        Service.builder()
            .name("Service-" + suffix)
            .serviceURL("http://localhost/api/services/1")
            .type("service-type")
            .blocked(false)
            .build();
    serviceRepository.save(service);

    cartography =
        Cartography.builder()
            .type("I")
            .name("Carto-" + suffix)
            .layers(List.of("Layer 1", "Layer 2"))
            .queryableFeatureAvailable(false)
            .queryableFeatureEnabled(false)
            .service(service)
            .availabilities(Collections.emptySet())
            .blocked(false)
            .build();
    cartography = cartographyRepository.save(cartography);

    cartographyStyleRepository.save(
        CartographyStyle.builder()
            .name("Style D")
            .cartography(cartography)
            .defaultStyle(true)
            .build());

    TreeNode treeNode1 = new TreeNode();
    treeNode1.setName("Non-Tree Node-" + suffix);
    treeNode1.setCartography(cartography);
    treeNode1.setTree(tree);
    nodes.add(treeNode1);

    TreeNode treeNode2 = new TreeNode();
    treeNode2.setName("Tree Node-" + suffix);
    treeNode2.setTree(publicTree);
    nodes.add(treeNode2);

    treeNodeRepository.saveAll(nodes);
  }

  @DisplayName("POST: Tree nodes cannot be created with non existent styles")
  @Disabled("Requires additional test data")
  @Test
  void nodesCantBeCreatedWithNonExistentStyles() throws Exception {
    TreeNode node = nodes.get(0);
    JSONObject json = new JSONObject();
    json.put("name", node.getName());
    json.put("tree", "/" + node.getTree().getId());
    json.put("cartography", "/" + cartography.getId());
    json.put("style", "Style D");
    mvc.perform(post(TREE_NODES_URI).content(json.toString()).with(user(Fixtures.admin())))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Tree node style not found in the tree node cartography's styles"));
  }

  @Test
  @DisplayName("POST: Tree nodes styles require a cartography")
  @WithMockUser(roles = "ADMIN")
  void nodesStylesRequireCartography() throws Exception {
    TreeNode node = nodes.get(0);
    JSONObject json = new JSONObject();
    json.put("name", node.getName());
    json.put("tree", "/" + node.getTree().getId());
    json.put("style", "Style D");
    mvc.perform(post(TREE_NODES_URI).content(json.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail").value("Tree node style requires a tree node with cartography"));
  }

  @Test
  @DisplayName("GET: Find nodes based on a list of trees")
  void findNodesBasedOnTreeList() {
    List<TreeNode> nodesFound = treeNodeRepository.findByTrees(trees);
    assertThat(nodesFound).containsExactlyInAnyOrderElementsOf(nodes);
  }
}
