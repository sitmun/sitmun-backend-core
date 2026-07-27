package org.sitmun.domain.cartography;

import static org.hamcrest.Matchers.containsString;
import static org.sitmun.test.URIConstants.CARTOGRAPHIES_URI;
import static org.sitmun.test.URIConstants.CARTOGRAPHY_STYLES_URI;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.cartography.style.CartographyStyle;
import org.sitmun.domain.cartography.style.CartographyStyleRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.TreeRepository;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Cartography DELETE integrity")
class CartographyDeleteResourceTest {

  @Autowired private MockMvc mvc;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private CartographyStyleRepository cartographyStyleRepository;
  @Autowired private TreeRepository treeRepository;
  @Autowired private TreeNodeRepository treeNodeRepository;
  @Autowired private TaskRepository taskRepository;

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DELETE unreferenced cartography created via POST returns 204")
  void deleteUnreferencedCartographyReturnsNoContent() throws Exception {
    Service service = saveService();
    String content =
        new JSONObject()
            .put("name", "delete-unreferenced-" + suffix())
            .put("layers", new JSONArray(List.of("layer-a")))
            .put("queryableLayers", new JSONArray())
            .put("selectableLayers", new JSONArray())
            .put("queryableFeatureAvailable", false)
            .put("queryableFeatureEnabled", false)
            .put("transparency", 0)
            .put("useAllStyles", false)
            .put("thematic", false)
            .put("service", "http://localhost/api/services/" + service.getId())
            .put("blocked", true)
            .toString();

    String location =
        mvc.perform(post(CARTOGRAPHIES_URI).contentType(APPLICATION_JSON).content(content))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");

    Integer id = TestUtils.extractId(location);
    // Accept header makes default SDR return a HAL body after DELETE; that path previously
    // LazyInitializationException'd on Cartography associations.
    mvc.perform(delete(CARTOGRAPHIES_URI + "/{id}", id).accept(MediaTypes.HAL_JSON))
        .andExpect(status().isNoContent());
  }

  @Test
  @Transactional
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DELETE cartography referenced by tree node returns 422")
  void deleteCartographyReferencedByTreeNodeReturnsUnprocessable() throws Exception {
    Cartography cartography = saveCartography();
    Tree tree = treeRepository.save(Tree.builder().name("tree-" + suffix()).build());
    TreeNode node = new TreeNode();
    node.setName("node-" + suffix());
    node.setTree(tree);
    node.setCartography(cartography);
    treeNodeRepository.save(node);

    mvc.perform(delete(CARTOGRAPHIES_URI + "/{id}", cartography.getId()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value(ProblemTypes.DATA_INTEGRITY_VIOLATION))
        .andExpect(
            jsonPath("$.properties.referencingEntityTranslationKey")
                .value("entity.tree-node.plural"));
  }

  @Test
  @Transactional
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DELETE cartography referenced by task returns 422")
  void deleteCartographyReferencedByTaskReturnsUnprocessable() throws Exception {
    Cartography cartography = saveCartography();
    taskRepository.save(Task.builder().name("task-" + suffix()).cartography(cartography).build());

    mvc.perform(delete(CARTOGRAPHIES_URI + "/{id}", cartography.getId()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value(ProblemTypes.DATA_INTEGRITY_VIOLATION))
        .andExpect(
            jsonPath("$.properties.referencingEntityTranslationKey").value("entity.task.plural"));
  }

  @Test
  @Transactional
  @WithMockUser(roles = "ADMIN")
  @DisplayName("DELETE cartography style in use by tree node returns 422")
  void deleteStyleInUseByTreeNodeReturnsUnprocessable() throws Exception {
    Cartography cartography = saveCartography();
    CartographyStyle style =
        cartographyStyleRepository.save(
            CartographyStyle.builder()
                .name("style-" + suffix())
                .cartography(cartography)
                .defaultStyle(false)
                .build());
    Tree tree = treeRepository.save(Tree.builder().name("tree-" + suffix()).build());
    TreeNode node = new TreeNode();
    node.setName("node-" + suffix());
    node.setTree(tree);
    node.setCartography(cartography);
    node.setStyle(style.getName());
    treeNodeRepository.save(node);

    mvc.perform(delete(CARTOGRAPHY_STYLES_URI + "/{id}", style.getId()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type").value(ProblemTypes.DATA_INTEGRITY_VIOLATION))
        .andExpect(jsonPath("$.detail", containsString("in use")))
        .andExpect(
            jsonPath("$.properties.referencingEntityTranslationKey")
                .value("entity.tree-node.plural"));
  }

  private Service saveService() {
    return serviceRepository.save(
        Service.builder()
            .name("svc-" + suffix())
            .serviceURL("http://localhost/api/services/probe")
            .type("WMS")
            .blocked(false)
            .build());
  }

  private Cartography saveCartography() {
    Service service = saveService();
    return cartographyRepository.save(
        Cartography.builder()
            .type("I")
            .name("carto-" + suffix())
            .layers(List.of("Layer1"))
            .queryableFeatureAvailable(false)
            .queryableFeatureEnabled(false)
            .service(service)
            .availabilities(Collections.emptySet())
            .blocked(false)
            .build());
  }

  private static String suffix() {
    return UUID.randomUUID().toString().substring(0, 8);
  }
}
