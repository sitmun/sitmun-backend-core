package org.sitmun.common.domain.tree.node;

import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.role.Role;
import org.sitmun.common.domain.role.RoleRepository;
import org.sitmun.common.domain.tree.Tree;
import org.sitmun.common.domain.tree.TreeRepository;
import org.sitmun.common.domain.tree.node.TreeNode;
import org.sitmun.common.domain.tree.node.TreeNodeRepository;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc

public class TreeNodeResourceTest {

  private static final String NON_PUBLIC_TREENODE_NAME = "Non-public Tree Node";
  private static final String PUBLIC_TREENODE_NAME = "Public Tree Node";
  private static final String PUBLIC_TREE_NAME = "Public Tree";
  private static final String NON_PUBLIC_TREE_NAME = "Non-public Tree Name";
  @Autowired
  TreeRepository treeRepository;
  @Autowired
  TreeNodeRepository treeNodeRepository;
  @Autowired
  RoleRepository roleRepository;
  @Autowired
  private MockMvc mvc;

  private Tree publicTree;
  private ArrayList<Tree> trees;
  private ArrayList<TreeNode> nodes;
  private Role publicRole;

  @BeforeEach
  public void init() {
    withMockSitmunAdmin(() -> {

      nodes = new ArrayList<>();

      publicRole = Role.builder().name("USUARIO_PUBLICO").build();
      roleRepository.save(publicRole);

      Set<Role> availableRoles = new HashSet<>();
      availableRoles.add(publicRole);

      trees = new ArrayList<>();

      publicTree = new Tree();
      publicTree.setName(PUBLIC_TREE_NAME);
      trees.add(publicTree);

      Tree tree = new Tree();
      tree.setName(NON_PUBLIC_TREE_NAME);
      trees.add(tree);
      treeRepository.saveAll(trees);

      publicTree.getAvailableRoles().addAll(availableRoles);
      treeRepository.save(publicTree);

      TreeNode treeNode1 = new TreeNode();
      treeNode1.setName(NON_PUBLIC_TREENODE_NAME);
      treeNode1.setTree(tree);
      nodes.add(treeNode1);

      TreeNode treeNode2 = new TreeNode();
      treeNode2.setName(PUBLIC_TREENODE_NAME);
      treeNode2.setTree(publicTree);

      nodes.add(treeNode2);
      treeNodeRepository.saveAll(nodes);
    });
  }

  @AfterEach
  public void cleanup() {
    withMockSitmunAdmin(() -> {
      for (TreeNode node : nodes) {
        treeNodeRepository.deleteById(node.getId());
      }
      for (Tree tree : trees) {
        treeRepository.deleteById(tree.getId());
      }
      roleRepository.delete(publicRole);
    });
  }

  @DisplayName("Tree nodes cannot be created with non existent styles")
  @Test
  public void nodesCantBeCreatedWithNonExistentStyles() throws Exception {
    TreeNode node = nodes.get(0);
    JSONObject json = new JSONObject();
    json.put("name", node.getName());
    json.put("tree", "/" + node.getTree().getId());
    json.put("cartography", "/0");
    json.put("style", "Style D");
    mvc.perform(post(URIConstants.TREE_NODES_URI).content(json.toString())
        .with(user(Fixtures.admin())))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Tree node style not found in the tree node cartography's styles"));
  }

  @DisplayName("Tree nodes styles require a cartography")
  @Test
  public void nodesStylesRequireCartography() throws Exception {
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
  @Disabled
  public void getPublicTreesAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(URIConstants.TREE_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.trees", hasSize(0)));
  }

  @Test
  @Disabled
  public void getPublicTreeNodesAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(URIConstants.TREE_URI + "/" + publicTree.getId() + "/nodes"))
      .andExpect(status().isOk());
  }

  @Test
  @Disabled
  public void getTreesAsTerritorialUser() {
    // TODO
    // ok is expected
  }

  @Test
  @Disabled
  public void getTreesAsSitmunAdmin() throws Exception {
    mvc.perform(get(URIConstants.TREE_URI)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.trees", hasSize(19)));
  }

  @Test
  @Disabled
  public void getTreesAsOrganizationAdmin() {
    // TODO
    // ok is expected
  }

  @Test
  @Disabled
  public void setAvailableRolesAsPublicFails() {
    // TODO
    // fail is expected
  }

  @Test
  @Disabled
  public void setAvailableRolesAsTerritorialUserFails() {
    // TODO
    // fail is expected
  }

  @Test
  @Disabled
  public void setAvailableRolesAsSitmunAdmin() {
    // TODO: Update available roles for the app as an admin user
    // ok is expected
  }

  @Test
  @Disabled
  public void setTreeAsSitmunAdmin() {
    // TODO: Update tree for the app as an admin user
    // ok is expected
  }

  @Test
  @Disabled
  public void setBackgroundAsSitmunAdmin() {
    // TODO: Update background for the app as an admin user
    // ok is expected
  }

  @Test
  @Disabled
  public void setAvailableRolesAsOrganizationAdmin() {
    // TODO: Update available roles for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Test
  @Disabled
  public void setTreeAsOrganizationAdmin() {
    // TODO: Update tree for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Test
  @Disabled
  public void setBackgroundAsOrganizationAdmin() {
    // TODO: Update background for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Test
  @Disabled
  public void setAvailableRolesAsOtherOrganizationAdminFails() {
    // TODO: Update available roles for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

  @Test
  @Disabled
  public void setTreeAsOtherOrganizationAdminFails() {
    // TODO: Update tree for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

  @Test
  @Disabled
  public void setBackgroundAsOtherOrganizationAdminFails() {
    // TODO: Update background for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

}