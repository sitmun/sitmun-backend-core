package org.sitmun.plugin.core.web.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.sitmun.plugin.core.domain.Role;
import org.sitmun.plugin.core.domain.Tree;
import org.sitmun.plugin.core.domain.TreeNode;
import org.sitmun.plugin.core.repository.RoleRepository;
import org.sitmun.plugin.core.repository.TreeNodeRepository;
import org.sitmun.plugin.core.repository.TreeRepository;
import org.sitmun.plugin.core.security.AuthoritiesConstants;
import org.sitmun.plugin.core.security.TokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.security.SecurityConstants.HEADER_STRING;
import static org.sitmun.plugin.core.security.SecurityConstants.TOKEN_PREFIX;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TreeNodeResourceTest {

  private static final String TREE_URI = "http://localhost/api/trees";
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
  TokenProvider tokenProvider;
  @Autowired
  private MockMvc mvc;

  private String token;
  private Tree publicTree;
  private ArrayList<Tree> trees;
  private ArrayList<TreeNode> nodes;
  private Role publicRole;

  @Before
  public void init() {
    withMockSitmunAdmin(() -> {

      token = tokenProvider.createToken(SITMUN_ADMIN_USERNAME);

      nodes = new ArrayList<>();

      publicRole = Role.builder().setName(AuthoritiesConstants.USUARIO_PUBLICO).build();
      roleRepository.save(publicRole);

      Set<Role> availableRoles = new HashSet<>();
      availableRoles.add(publicRole);

      trees = new ArrayList<>();

      publicTree = new Tree();
      publicTree.setName(PUBLIC_TREE_NAME);
      publicTree.setAvailableRoles(availableRoles);
      trees.add(publicTree);

      Tree tree = new Tree();
      tree.setName(NON_PUBLIC_TREE_NAME);
      trees.add(tree);
      treeRepository.saveAll(trees);

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

  @After
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

  @Ignore
  public void getPublicTreesAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(TREE_URI))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.trees", hasSize(0)));
  }

  @Ignore
  public void getPublicTreeNodesAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(TREE_URI + "/" + publicTree.getId() + "/nodes"))
      .andExpect(status().isOk());
  }

  @Ignore
  public void getTreesAsTerritorialUser() {
    // TODO
    // ok is expected
  }

  @Test
  public void getTreesAsSitmunAdmin() throws Exception {
    mvc.perform(get(TREE_URI)
      .header(HEADER_STRING, TOKEN_PREFIX + token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.trees", hasSize(14)));
  }

  @Ignore
  public void getTreesAsOrganizationAdmin() {
    // TODO
    // ok is expected
  }

  @Ignore
  public void setAvailableRolesAsPublicFails() {
    // TODO
    // fail is expected
  }

  @Ignore
  public void setAvailableRolesAsTerritorialUserFails() {
    // TODO
    // fail is expected
  }

  @Ignore
  public void setAvailableRolesAsSitmunAdmin() {
    // TODO: Update available roles for the app as an admin user
    // ok is expected
  }

  @Ignore
  public void setTreeAsSitmunAdmin() {
    // TODO: Update tree for the app as an admin user
    // ok is expected
  }

  @Ignore
  public void setBackgroundAsSitmunAdmin() {
    // TODO: Update background for the app as an admin user
    // ok is expected
  }

  @Ignore
  public void setAvailableRolesAsOrganizationAdmin() {
    // TODO: Update available roles for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Ignore
  public void setTreeAsOrganizationAdmin() {
    // TODO: Update tree for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Ignore
  public void setBackgroundAsOrganizationAdmin() {
    // TODO: Update background for the app (linked to the same organization) as an organization admin user
    // ok is expected
  }

  @Ignore
  public void setAvailableRolesAsOtherOrganizationAdminFails() {
    // TODO: Update available roles for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

  @Ignore
  public void setTreeAsOtherOrganizationAdminFails() {
    // TODO: Update tree for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

  @Ignore
  public void setBackgroundAsOtherOrganizationAdminFails() {
    // TODO: Update background for the app (linked to another organization) as an organization admin user
    // fail is expected
  }

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

}
