package org.sitmun.plugin.core.web.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
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
  private Tree tree;
  private Tree publicTree;
  private Role publicRole;

  @Before
  public void init() {
    ArrayList<TreeNode> nodesToCreate = new ArrayList<TreeNode>();
    publicRole = this.roleRepository.findOneByName(AuthoritiesConstants.USUARIO_PUBLICO).get();
    Set<Role> availableRoles = new HashSet<Role>();
    availableRoles.add(publicRole);

    ArrayList<Tree> treesToCreate = new ArrayList<Tree>();

    publicTree = new Tree();
    publicTree.setName(PUBLIC_TREE_NAME);
    publicTree.setAvailableRoles(availableRoles);
    treesToCreate.add(publicTree);


    Set<Tree> trees = new HashSet<Tree>();
    trees.add(publicTree);

    tree = new Tree();
    tree.setName(NON_PUBLIC_TREE_NAME);
    treesToCreate.add(tree);
    this.treeRepository.save(treesToCreate);

    TreeNode treeNode1 = new TreeNode();
    treeNode1.setName(NON_PUBLIC_TREENODE_NAME);
    treeNode1.setTree(tree);
    nodesToCreate.add(treeNode1);
    TreeNode treeNode2 = new TreeNode();
    treeNode2.setName(PUBLIC_TREENODE_NAME);
    treeNode2.setTree(publicTree);
    nodesToCreate.add(treeNode2);
    treeNodeRepository.save(nodesToCreate);

  }

  @After
  public void cleanup() {
    treeRepository.deleteAll();
    //treeNodeRepository.deleteAll();
  }

  @Test
  public void getPublicTreesAsPublic() throws Exception {
    // TO DO
    // ok is expected
    mvc.perform(get(TREE_URI)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.trees", hasSize(1)));
  }
	
	/*
	@Test
	public void getPublicTreeNodesAsPublic() throws Exception {
		// TO DO
		// ok is expected
		mvc.perform(get(TREE_URI+"/2/nodes")).andDo(print()).andExpect(status().isOk());
	}
	*/


  @Test
  public void getTreesAsTerritorialUser() {
    // TO DO
    // ok is expected
  }

  @Test
  @WithMockUser(username = ADMIN_USERNAME)
  public void getTreesAsSitmunAdmin() throws Exception {
    // ok is expected
    mvc.perform(get(TREE_URI)).andDo(print()).andExpect(status().isOk()).andExpect(jsonPath("$._embedded.trees", hasSize(2)));

  }

  @Test
  public void getTreesAsOrganizationAdmin() {
    // TO DO
    // ok is expected
  }

  @Test
  public void setAvailableRolesAsPublicFails() {
    // TO DO
    // fail is expected
  }

  @Test
  public void setAvailableRolesAsTerritorialUserFails() {
    // TO DO
    // fail is expected
  }

  @Test
  public void setAvailableRolesAsSitmunAdmin() {
    // TO DO
    // Update available roles for the app as an admin user
    // ok is expected
  }

  @Test
  public void setTreeAsSitmunAdmin() {
    // TO DO
    // Update tree for the app as an admin user
    // ok is expected
  }

  @Test
  public void setBackgroundAsSitmunAdmin() {
    // TO DO
    // Update background for the app as an admin user
    // ok is expected
  }

  @Test
  public void setAvailableRolesAsOrganizationAdmin() {
    // TO DO
    // Update available roles for the app (linked to the same organization) as an
    // organization admin user
    // ok is expected
  }

  @Test
  public void setTreeAsOrganizationAdmin() {
    // TO DO
    // Update tree for the app (linked to the same organization) as an organization
    // admin user
    // ok is expected
  }

  @Test
  public void setBackgroundAsOrganizationAdmin() {
    // TO DO
    // Update background for the app (linked to the same organization) as an
    // organization admin user
    // ok is expected
  }

  @Test
  public void setAvailableRolesAsOtherOrganizationAdminFails() {
    // TO DO
    // Update available roles for the app (linked to another organization) as an
    // organization admin user
    // fail is expected
  }

  @Test
  public void setTreeAsOtherOrganizationAdminFails() {
    // TO DO
    // Update tree for the app (linked to another organization) as an organization
    // admin user
    // fail is expected
  }

  @Test
  public void setBackgroundAsOtherOrganizationAdminFails() {
    // TO DO
    // Update background for the app (linked to another organization) as an
    // organization admin user
    // fail is expected
  }

}
