package org.sitmun.domain.tree;

import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.application.tree.ApplicationTree;
import org.sitmun.domain.application.tree.ApplicationTreeRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tree association Data REST test")
class TreeAssociationResourceTest {

  @Autowired private MockMvc mvc;
  @Autowired private TreeRepository treeRepository;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private ApplicationTreeRepository applicationTreeRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private UserRepository userRepository;

  private Tree tree;
  private Application application;
  private ApplicationTree applicationTree;
  private Role role;
  private User owner;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void init() {
    role = roleRepository.save(Role.builder().name("tree-assoc-role").build());
    owner =
        userRepository.save(
            User.builder()
                .username("tree-assoc-owner")
                .password("unused")
                .firstName("Tree")
                .lastName("Owner")
                .email("tree-owner@example.com")
                .administrator(false)
                .blocked(false)
                .build());
    tree = treeRepository.save(Tree.builder().name("tree-assoc").type("cartography").build());
    application =
        applicationRepository.save(
            Application.builder()
                .name("tree-assoc-app")
                .type("I")
                .jspTemplate("")
                .createdDate(Date.from(Instant.now()))
                .build());
    applicationTree =
        applicationTreeRepository.save(
            ApplicationTree.builder().application(application).tree(tree).order(0).build());
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    if (applicationTree != null && applicationTree.getId() != null) {
      applicationTreeRepository.deleteById(applicationTree.getId());
    }
    if (application != null && application.getId() != null) {
      applicationRepository.deleteById(application.getId());
    }
    if (tree != null && tree.getId() != null) {
      treeRepository.deleteById(tree.getId());
    }
    if (role != null && role.getId() != null) {
      roleRepository.deleteById(role.getId());
    }
    if (owner != null && owner.getId() != null) {
      userRepository.deleteById(owner.getId());
    }
  }

  @Test
  @DisplayName("PUT: owner association succeeds when tree has application links")
  @WithMockUser(roles = "ADMIN")
  void putOwnerAssociationWhenTreeHasApplicationLinks() throws Exception {
    String content = USER_ITEM_URI.replace("{0}", owner.getId().toString());
    mvc.perform(
            put(TREE_URI_OWNER, tree.getId()).content(content).contentType("text/uri-list"))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("PUT: availableRoles association succeeds when tree has application links")
  @WithMockUser(roles = "ADMIN")
  void putAvailableRolesAssociationWhenTreeHasApplicationLinks() throws Exception {
    String content = ROLE_URI.replace("{0}", role.getId().toString());
    mvc.perform(
            put(TREE_URI_AVAILABLE_ROLES, tree.getId())
                .content(content)
                .contentType("text/uri-list"))
        .andExpect(status().isNoContent());
  }
}
