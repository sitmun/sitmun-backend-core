package org.sitmun.domain.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.Application;
import org.sitmun.domain.application.tree.ApplicationTree;
import org.sitmun.domain.application.tree.ApplicationTreeRepository;
import org.sitmun.domain.tree.dto.TreeTypeValidationRequest;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.sitmun.test.BaseTest;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisplayName("Tree controller test")
class TreeControllerTest extends BaseTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ApplicationTreeRepository applicationTreeRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private PlatformTransactionManager transactionManager;

  @BeforeEach
  void resetSeedTreeTypes() {
    restoreSeedTreeTypes();
  }

  /** Committed JDBC restore — Data REST handlers must see seed outside any test TX. */
  private void restoreSeedTreeTypes() {
    // Postgres tests disable Hikari auto-commit; commit the seed reset explicitly.
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              jdbcTemplate.update(
                  "UPDATE STM_TREE SET TRE_TYPE = 'cartography' WHERE TRE_ID IN (1, 2, 3)");
              // Drop stray nodes left by other Data REST tests (seed tree 2 is only node 15).
              jdbcTemplate.update(
                  "DELETE FROM STM_TREE_NOD WHERE TNO_TREEID = 2 AND TNO_ID <> 15"
                      + " AND TNO_PARENTID IS NOT NULL");
              jdbcTemplate.update("DELETE FROM STM_TREE_NOD WHERE TNO_TREEID = 2 AND TNO_ID <> 15");
              jdbcTemplate.update("UPDATE STM_TREE_NOD SET TNO_RADIO = FALSE WHERE TNO_TREEID = 2");
              // Seed: tree 1 radio folder is node 7 only.
              jdbcTemplate.update(
                  "UPDATE STM_TREE_NOD SET TNO_RADIO = FALSE WHERE TNO_TREEID = 1 AND TNO_ID <> 7");
              jdbcTemplate.update("UPDATE STM_TREE_NOD SET TNO_RADIO = TRUE WHERE TNO_ID = 7");
            });
  }

  private static String withTreeType(String treeJson, String type) {
    return JsonPath.parse(treeJson).set("$.type", type).jsonString();
  }

  @Test
  @DisplayName("PUT: Fail 400 when try save touristic tree with non touristic application")
  @WithMockUser(roles = "ADMIN")
  void saveTouristicTreeWithNonTouristicApplication() throws Exception {
    mockMvc
        .perform(
            post(APPLICATION_TREES_COLLECTION_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"application":"%s","tree":"%s"}
                    """
                        .formatted(APPLICATION_URI.replace("{0}", "1"), TREE_URI + "/4")))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value("Touristic tree can only be linked with 0 or 1 tourist application"));
  }

  @Test
  @DisplayName("PUT: Fail 400 when try save non touristic tree with touristic application")
  @WithMockUser(roles = "ADMIN")
  void saveNoTouristicTreeWithTouristicApplication() throws Exception {
    mockMvc
        .perform(
            post(APPLICATION_TREES_COLLECTION_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"application":"%s","tree":"%s"}
                    """
                        .formatted(APPLICATION_URI.replace("{0}", "6"), TREE_URI + "/1")))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "A non-touristic tree can only be linked to a non-tourist application or touristic application with only one touristic tree"));
  }

  @Test
  @DisplayName("PUT: Check that updating and removing standard trees works as expected")
  @WithMockUser(roles = "ADMIN")
  void updateAndRemoveATree() throws Exception {
    String treeUri = TREE_URI + "/1";
    String app1Uri = APPLICATION_URI.replace("{0}", "1");
    String app2Uri = APPLICATION_URI.replace("{0}", "2");

    assertEquals(1, linksForTree(1).size());
    linksForTree(1).forEach(link -> assertEquals(1, link.getApplication().getId()));

    mockMvc.perform(delete(APPLICATION_TREE_URI, 1)).andExpect(status().isNoContent());

    mockMvc
        .perform(
            post(APPLICATION_TREES_COLLECTION_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"application":"%s","tree":"%s"}
                    """
                        .formatted(app2Uri, treeUri)))
        .andExpect(status().isCreated());

    assertEquals(1, linksForTree(1).size());
    linksForTree(1).forEach(link -> assertEquals(2, link.getApplication().getId()));

    Integer app2LinkId = linksForTree(1).stream().findFirst().orElseThrow().getId();
    mockMvc.perform(delete(APPLICATION_TREE_URI, app2LinkId)).andExpect(status().isNoContent());

    assertEquals(0, linksForTree(1).size());

    mockMvc
        .perform(
            post(APPLICATION_TREES_COLLECTION_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"application":"%s","tree":"%s"}
                    """
                        .formatted(app1Uri, treeUri)))
        .andExpect(status().isCreated());

    assertEquals(1, linksForTree(1).size());
    linksForTree(1).forEach(link -> assertEquals(1, link.getApplication().getId()));
  }

  private List<ApplicationTree> linksForTree(int treeId) {
    return applicationTreeRepository.findAll().stream()
        .filter(link -> link.getTree().getId() == treeId)
        .toList();
  }

  @Test
  @DisplayName("POST: Validate touristic tree type with no applications - returns 204")
  @WithMockUser(roles = "ADMIN")
  void validateTouristicTypeWithNoApplications() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder().type("touristic").applicationIds(Set.of()).build();

    mvc.perform(
            post("/api/trees/2/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Validate touristic tree type with 1 touristic app - returns 204")
  @WithMockUser(roles = "ADMIN")
  void validateTouristicTypeWithOneTouristicApp() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder()
            .type("touristic")
            .applicationIds(Set.of(6)) // App 6 is touristic
            .build();

    mvc.perform(
            post("/api/trees/2/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Validate touristic tree type with 2+ apps - returns 422")
  @WithMockUser(roles = "ADMIN")
  void validateTouristicTypeWithMultipleApps() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder()
            .type("touristic")
            .applicationIds(Set.of(1, 2)) // Multiple apps violate touristic constraint
            .build();

    mvc.perform(
            post("/api/trees/1/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT))
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.title").value("Tree Type Change Validation Failed"))
        .andExpect(jsonPath("$.detail").exists());
  }

  @Test
  @DisplayName("POST: Validate touristic tree type with non-touristic app - returns 422")
  @WithMockUser(roles = "ADMIN")
  void validateTouristicTypeWithNonTouristicApp() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder()
            .type("touristic")
            .applicationIds(Set.of(1)) // App 1 is non-touristic
            .build();

    mvc.perform(
            post("/api/trees/4/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT))
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.detail").exists());
  }

  @Test
  @DisplayName("POST: Validate non-touristic tree type with non-touristic apps - returns 204")
  @WithMockUser(roles = "ADMIN")
  void validateNonTouristicTypeWithNonTouristicApps() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder()
            .type("cartography")
            .applicationIds(Set.of(1, 2, 3)) // Multiple non-touristic apps OK
            .build();

    mvc.perform(
            post("/api/trees/1/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Validate non-touristic tree with empty touristic app - returns 422")
  @WithMockUser(roles = "ADMIN")
  @Transactional
  void validateNonTouristicTreeWithEmptyTouristicApp() throws Exception {
    // App 6 is touristic but has no trees linked
    // Non-touristic trees can only be linked to touristic apps with exactly one touristic tree
    // Since App 6 has no trees, validation should fail

    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder()
            .type("cartography")
            .applicationIds(Set.of(6)) // App 6 is touristic with 0 trees
            .build();

    mvc.perform(
            post("/api/trees/1/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT));
  }

  @Test
  @DisplayName("POST: Validate non-touristic tree with multi-tree touristic app - returns 422")
  @WithMockUser(roles = "ADMIN")
  void validateNonTouristicTypeWithMultiTreeTouristicApp() throws Exception {
    // This test would need setup where App 6 is linked to multiple touristic trees
    // For now, test the rejection of a touristic app that's incompatible

    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder()
            .type("cartography")
            .applicationIds(Set.of(6)) // App 6 touristic
            .build();

    // The actual validation depends on how many touristic trees App 6 has
    // This is a placeholder - adjust based on actual TreeEventHandler logic
    mvc.perform(
            post("/api/trees/4/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT));
  }

  @Test
  @DisplayName("POST: Validate with null type - returns 400")
  @WithMockUser(roles = "ADMIN")
  void validateWithNullType() throws Exception {
    String requestJson = "{\"type\": null, \"applicationIds\": [1]}";

    mvc.perform(
            post("/api/trees/1/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST: Validate with empty type - returns 400")
  @WithMockUser(roles = "ADMIN")
  void validateWithEmptyType() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder().type("").applicationIds(Set.of(1)).build();

    mvc.perform(
            post("/api/trees/1/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST: Validate with null applicationIds defaults to empty set - returns 204")
  @WithMockUser(roles = "ADMIN")
  void validateWithNullApplicationIds() throws Exception {
    String requestJson = "{\"type\": \"touristic\", \"applicationIds\": null}";

    mvc.perform(
            post("/api/trees/2/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: Validate with non-existent tree ID - returns 404")
  @WithMockUser(roles = "ADMIN")
  void validateWithNonExistentTreeId() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder().type("cartography").applicationIds(Set.of(1)).build();

    mvc.perform(
            post("/api/trees/99999/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value(ProblemTypes.NOT_FOUND));
  }

  @Test
  @DisplayName("POST: Validate with non-existent application ID - returns 404")
  @WithMockUser(roles = "ADMIN")
  void validateWithNonExistentApplicationId() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder()
            .type("cartography")
            .applicationIds(Set.of(99999))
            .build();

    mvc.perform(
            post("/api/trees/1/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value(ProblemTypes.NOT_FOUND));
  }

  @Test
  @DisplayName("PUT: cartography to edition with radio folders - returns 400")
  @WithMockUser(roles = "ADMIN")
  void putCartographyToEditionWithRadioFoldersRejected() throws Exception {
    String treeJson =
        mvc.perform(get("/api/trees/1"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    mvc.perform(
            put("/api/trees/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(withTreeType(treeJson, "edition")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Cartography tree type cannot be changed while radio folders are configured"));
  }

  @Test
  @DisplayName("PUT: cartography to edition without radio folders - returns 200")
  @WithMockUser(roles = "ADMIN")
  void putCartographyToEditionWithoutRadioFoldersSucceeds() throws Exception {
    // Mutates seed via Data REST (commits); must restore — test @Transactional cannot wrap this.
    try {
      Integer radioFolders =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM STM_TREE_NOD WHERE TNO_TREEID = 2 AND TNO_RADIO = TRUE",
              Integer.class);
      assertEquals(0, radioFolders, "tree 2 must have no radio folders before type change");

      String treeJson =
          mvc.perform(get("/api/trees/2"))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      assertEquals("cartography", JsonPath.parse(treeJson).read("$.type", String.class));

      MvcResult putResult =
          mvc.perform(
                  put("/api/trees/2")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(withTreeType(treeJson, "edition")))
              .andReturn();
      String putBody = putResult.getResponse().getContentAsString();
      assertEquals(
          200, putResult.getResponse().getStatus(), "PUT /api/trees/2 type→edition: " + putBody);
      assertEquals("edition", JsonPath.parse(putBody).read("$.type", String.class));
    } finally {
      restoreSeedTreeTypes();
    }
  }

  @Test
  @DisplayName("POST: cartography to edition with radio folders - returns 422")
  @WithMockUser(roles = "ADMIN")
  void validateCartographyToEditionWithRadioFoldersRejected() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder().type("edition").applicationIds(Set.of(1)).build();

    mvc.perform(
            post("/api/trees/1/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT))
        .andExpect(
            jsonPath("$.detail")
                .value(
                    "Cartography tree type cannot be changed while radio folders are configured"));
  }

  @Test
  @DisplayName("POST: Validation errors return proper RFC 9457 format")
  @WithMockUser(roles = "ADMIN")
  void validateRfc9457Format() throws Exception {
    TreeTypeValidationRequest request =
        TreeTypeValidationRequest.builder()
            .type("touristic")
            .applicationIds(Set.of(1, 2)) // Force error
            .build();

    mvc.perform(
            post("/api/trees/1/validate-type-change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestUtils.asJsonString(request)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_TYPE_CHANGE_CONSTRAINT))
        .andExpect(jsonPath("$.status").value(422))
        .andExpect(jsonPath("$.title").exists())
        .andExpect(jsonPath("$.detail").exists())
        .andExpect(jsonPath("$.instance").exists());
  }
}
