package org.sitmun.domain.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.Application;
import org.sitmun.test.BaseTest;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("Tree controller test")
class TreeControllerTest extends BaseTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private TreeRepository treeRepository;

  @Test
  @DisplayName("PUT: Fail 400 when try save touristic tree with non touristic application")
  void saveTouristicTreeWithNonTouristicApplication() throws Exception {
    // From
    String applications = "http://localhost/api/applications/1";

    mockMvc
        .perform(
            put(URIConstants.TREE_AVAILABLE_APPLICATIONS_URI, 4)
                .content(applications)
                .header("Content-Type", "text/uri-list")
                .with(user(Fixtures.admin())))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Touristic tree only can be linked with 0..1 tourist application"));
  }

  @Test
  @DisplayName("PUT: Fail 400 when try save non touristic tree with touristic application")
  void saveNoTouristicTreeWithTouristicApplication() throws Exception {
    // From
    String applications = "http://localhost/api/applications/6";

    mockMvc
        .perform(
            put(URIConstants.TREE_AVAILABLE_APPLICATIONS_URI, 1)
                .content(applications)
                .header("Content-Type", "text/uri-list")
                .with(user(Fixtures.admin())))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "A non touristic tree can only be linked to a non tourist application or touristic application with only one touristic tree"));
  }

  @Test
  @DisplayName("PUT: Check that updating and removing standard trees works as expected")
  @Transactional
  void updateAndRemoveATree() throws Exception {
    // From
    String newApplication = "http://localhost/api/applications/2";
    String oldApplication = "http://localhost/api/applications/1";

    Tree tree = treeRepository.findOneWithEagerRelationships(1);
    assertEquals(1, tree.getAvailableApplications().size());
    tree.getAvailableApplications().forEach(app -> assertEquals(1, app.getId()));

    mockMvc
        .perform(
            put(URIConstants.TREE_AVAILABLE_APPLICATIONS_URI, 1)
                .content(newApplication)
                .header("Content-Type", "text/uri-list")
                .with(user(Fixtures.admin())))
        .andExpect(status().isNoContent());

    tree = treeRepository.findOneWithEagerRelationships(1);
    assertEquals(1, tree.getAvailableApplications().size());
    tree.getAvailableApplications().forEach(app -> assertEquals(2, app.getId()));

    mockMvc
        .perform(
            put(URIConstants.TREE_AVAILABLE_APPLICATIONS_URI, 1)
                .header("Content-Type", "text/uri-list")
                .with(user(Fixtures.admin())))
        .andExpect(status().isNoContent());

    tree = treeRepository.findOneWithEagerRelationships(1);
    assertEquals(0, tree.getAvailableApplications().size());

    mockMvc
        .perform(
            put(URIConstants.TREE_AVAILABLE_APPLICATIONS_URI, 1)
                .content(oldApplication)
                .header("Content-Type", "text/uri-list")
                .with(user(Fixtures.admin())))
        .andExpect(status().isNoContent());

    tree = treeRepository.findOneWithEagerRelationships(1);
    assertEquals(1, tree.getAvailableApplications().size());
    tree.getAvailableApplications().forEach(app -> assertEquals(1, app.getId()));
  }
}
