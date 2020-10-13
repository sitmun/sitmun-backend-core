package org.sitmun.plugin.core.web.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
public class TreeRestResourceIntTest {

  private static final String ADMIN_USERNAME = "admin";
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
  private Tree publicTree;

  @Before
  public void init() {
    ArrayList<TreeNode> nodesToCreate = new ArrayList<>();
    Role publicRole = this.roleRepository.findOneByName(AuthoritiesConstants.USUARIO_PUBLICO).get();
    Set<Role> availableRoles = new HashSet<>();
    availableRoles.add(publicRole);

    ArrayList<Tree> treesToCreate = new ArrayList<>();

    publicTree = new Tree();
    publicTree.setName(PUBLIC_TREE_NAME);
    publicTree.setAvailableRoles(availableRoles);
    treesToCreate.add(publicTree);


    Set<Tree> trees = new HashSet<>();
    trees.add(publicTree);

    Tree tree = new Tree();
    tree.setName(NON_PUBLIC_TREE_NAME);
    treesToCreate.add(tree);
    this.treeRepository.saveAll(treesToCreate);

    TreeNode treeNode1 = new TreeNode();
    treeNode1.setName(NON_PUBLIC_TREENODE_NAME);
    treeNode1.setTree(tree);
    nodesToCreate.add(treeNode1);
    TreeNode treeNode2 = new TreeNode();
    treeNode2.setName(PUBLIC_TREENODE_NAME);
    treeNode2.setTree(publicTree);
    nodesToCreate.add(treeNode2);
    treeNodeRepository.saveAll(nodesToCreate);

  }

  @Test
  public void getPublicTreesAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(TREE_URI))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.trees", hasSize(1)));
  }

  @Ignore
  public void getPublicTreeNodesAsPublic() throws Exception {
    // TODO
    // ok is expected
    mvc.perform(get(TREE_URI + "/" + publicTree.getId() + "/nodes"))
        .andDo(print())
        .andExpect(status().isOk());
  }

  @Test
  public void getTreesAsTerritorialUser() {
    // TODO
    // ok is expected
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void getTreesAsSitmunAdmin() throws Exception {
    mvc.perform(get(TREE_URI))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.trees", hasSize(2)));
  }

  @Test
  public void getTreesAsOrganizationAdmin() {
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
    // TODO: Update available roles for the app as an admin user
    // ok is expected
  }

  @Test
  public void setTreeAsSitmunAdmin() {
    // TODO: Update tree for the app as an admin user
    // ok is expected
  }

  @Test
  public void setBackgroundAsSitmunAdmin() {
    // TODO: Update background for the app as an admin user
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
