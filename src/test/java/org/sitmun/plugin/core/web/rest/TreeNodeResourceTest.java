package org.sitmun.plugin.core.web.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Role;
import org.sitmun.plugin.core.domain.Tree;
import org.sitmun.plugin.core.domain.TreeNode;
import org.sitmun.plugin.core.repository.RoleRepository;
import org.sitmun.plugin.core.repository.TreeNodeRepository;
import org.sitmun.plugin.core.repository.TreeRepository;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

      publicRole = Role.builder().name(AuthoritiesConstants.USUARIO_PUBLICO).build();
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
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME)))
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
