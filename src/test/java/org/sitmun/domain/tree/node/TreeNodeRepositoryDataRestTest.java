package org.sitmun.domain.tree.node;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.image.ImageDataUri;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Tree Node Repository Data REST test")
class TreeNodeRepositoryDataRestTest {

  private static final String PNG_8X8_TRANSPARENT =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII";
  private static final String PNG_125X125_TRANSPARENT =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAH0AAAB9AQAAAACn+1GIAAAAIElEQVR4XmP4jwp+MIwKjAqMCowKjAqMCowKjAqQIAAAMVDFL8q1f5EAAAAASUVORK5CYII=";

  @Autowired private MockMvc mvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetSeedTreeNodeState() {
    jdbcTemplate.update("DELETE FROM STM_TREE_NOD WHERE TNO_ID > 14");
    jdbcTemplate.update(
        "UPDATE STM_TREE_NOD SET TNO_ACTIVE = TRUE, TNO_DEFAULT = FALSE, TNO_RADIO = FALSE,"
            + " TNO_LOAD_DATA = FALSE");
    jdbcTemplate.update(
        "UPDATE STM_TREE_NOD SET TNO_RADIO = TRUE, TNO_LOAD_DATA = TRUE WHERE TNO_ID = 7");
    jdbcTemplate.update("UPDATE STM_TREE_NOD SET TNO_DEFAULT = TRUE WHERE TNO_ID = 9");
    jdbcTemplate.update("UPDATE STM_TREE_NOD SET TNO_ACTIVE = FALSE WHERE TNO_ID IN (12, 13)");
  }

  @Test
  @DisplayName("GET: Retrieve tree name from node")
  @WithMockUser(roles = "ADMIN")
  void retrieveTreeName() throws Exception {
    mvc.perform(get(TREE_NODE_URI_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.treeName").value("Provincial"));
  }

  @Test
  @DisplayName("GET: Retrieve folder")
  @WithMockUser(roles = "ADMIN")
  void retrieveFolder() throws Exception {
    mvc.perform(get(TREE_NODE_URI_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isFolder").value(true));
    mvc.perform(get(TREE_NODE_CARTOGRAPHY_URI, 1)).andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET: Retrieve leaf")
  @WithMockUser(roles = "ADMIN")
  void retrieveLeaf() throws Exception {
    mvc.perform(get(TREE_NODE_URI_PROJECTION, 3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isFolder").value(false));
    mvc.perform(get(TREE_NODE_CARTOGRAPHY_URI, 3)).andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET: Retrieve nodes from tree")
  @WithMockUser(roles = "ADMIN")
  void retrieveNodesFromTree() throws Exception {
    mvc.perform(get(TREE_ALL_NODES_URI, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tree-nodes", hasSize(14)))
        .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == true)]", hasSize(6)))
        .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == false)]", hasSize(8)));
  }

  @Test
  @DisplayName("POST: New nodes can be posted")
  @WithMockUser(roles = "ADMIN")
  void newTreeNodesCanBePosted() throws Exception {
    String content =
        """
      {
      "name":"test",
      "tree":"http://localhost/api/trees/1"
      }
      """;

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("test"))
            .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isOk());

    mvc.perform(delete(TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("POST: New nodes with parent can be posted")
  @WithMockUser(roles = "ADMIN")
  void newTreeNodesWithParentCanBePosted() throws Exception {
    String content =
        """
        {
        "name":"test",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/1"
        }""";

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("test"))
            .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isOk());

    mvc.perform(get(TREE_NODE_PARENT_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isOk());

    mvc.perform(delete(TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("POST: New nodes with image data can be posted and resized")
  @WithMockUser(roles = "ADMIN")
  void newTreeNodesWithImageDataCanBePostedAndResized() throws Exception {
    String content =
        """
        {
        "name":"test",
        "tree":"http://localhost/api/trees/1",
        "image":"%s"
        }"""
            .formatted(PNG_8X8_TRANSPARENT);

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.image").value(startsWith(PNG_125X125_TRANSPARENT)))
            .andExpect(jsonPath("$.name").value("test"))
            .andReturn();

    String response = result.getResponse().getContentAsString();
    validateSizeOfResponseImage(response, 125, 125);

    mvc.perform(get(TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isOk());

    mvc.perform(delete(TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("POST: New nodes with image data with right size are not resized")
  @WithMockUser(roles = "ADMIN")
  void newTreeNodesWithImageDataWithRightSizeAreNotResized() throws Exception {
    String content =
        """
        {
        "name":"test",
        "tree":"http://localhost/api/trees/1",
        "image":"%s"
        }"""
            .formatted(PNG_125X125_TRANSPARENT);

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.image").value(startsWith(PNG_125X125_TRANSPARENT)))
            .andExpect(jsonPath("$.name").value("test"))
            .andReturn();

    String response = result.getResponse().getContentAsString();
    validateSizeOfResponseImage(response, 125, 125);

    mvc.perform(get(TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isOk());

    mvc.perform(delete(TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("POST: New nodes with image can be posted")
  @WithMockUser(roles = "ADMIN")
  void newTreeNodesWithImageUriCanBePosted() throws Exception {
    String content =
        """
        {
        "name":"test",
        "tree":"http://localhost/api/trees/1",
        "image":"https://avatars.githubusercontent.com/u/24718368?s=96&v=4"
        }""";

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.image").value(startsWith("data:image/png;base64,iVBOR")))
            .andExpect(jsonPath("$.name").value("test"))
            .andReturn();

    String response = result.getResponse().getContentAsString();
    validateSizeOfResponseImage(response, 125, 125);

    mvc.perform(get(TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isOk());

    mvc.perform(delete(TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("POST: New nodes with image with extension can be posted")
  @WithMockUser(roles = "ADMIN")
  void newTreeNodesWithImageWithExtensionUriCanBePosted() throws Exception {
    // Unique name to avoid shared-DB issues when suite runs in parallel or multiple contexts
    String uniqueName = "test-img-" + System.nanoTime();
    // %% in URL so .formatted() does not interpret %20 as format specifiers
    String content =
        """
        {
        "name":"%s",
        "tree":"http://localhost/api/trees/1",
        "image":"https://raw.githubusercontent.com/sitmun/community/master/logotip%%20SITMUN%%20JPG/horitzontal/01.principal-horit-normal.jpg"
        }"""
            .formatted(uniqueName);

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.image").value(startsWith("data:image/jpeg;base64,/9j/4AAQSkZJRg")))
            .andExpect(jsonPath("$.name").value(uniqueName))
            .andReturn();

    String response = result.getResponse().getContentAsString();
    validateSizeOfResponseImage(response, 125, 125);

    mvc.perform(get(TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isOk());

    mvc.perform(delete(TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
        .andExpect(status().isNoContent())
        .andReturn();
  }

  @Test
  @DisplayName("PATCH: active round-trips through projection=view on cartography leaf")
  @WithMockUser(roles = "ADMIN")
  void activeRoundTripsThroughProjection() throws Exception {
    String patchContent =
        """
        {
        "active": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 3).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  @DisplayName("PATCH: visible round-trips through projection=view")
  @WithMockUser(roles = "ADMIN")
  void visibleRoundTripsThroughProjection() throws Exception {
    String patchContent =
        """
        {
        "visible": false
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 3).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visible").value(false));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.visible").value(false));
  }

  @Test
  @DisplayName("POST: active is cleared on folder nodes without cartography")
  @WithMockUser(roles = "ADMIN")
  void activeClearedOnFolderWithoutCartography() throws Exception {
    String content =
        """
        {
        "name":"load-by-default-folder",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/1",
        "active": true
        }
        """;

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(false))
            .andReturn();

    Integer id =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Integer.class);
    mvc.perform(delete(TREE_NODE_URI, id)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: active is cleared when visible is false")
  @WithMockUser(roles = "ADMIN")
  void activeClearedWhenVisibleIsFalse() throws Exception {
    String content =
        """
        {
        "name":"hidden-load-by-default-leaf",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/7",
        "cartography":"http://localhost/api/cartographies/8",
        "visible": false,
        "active": true
        }
        """;

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(false))
            .andReturn();

    Integer id =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Integer.class);
    mvc.perform(delete(TREE_NODE_URI, id)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: active is cleared on cartography+task malformed leaf")
  @WithMockUser(roles = "ADMIN")
  void activeClearedOnCartographyTaskMalformedLeaf() throws Exception {
    String content =
        """
        {
        "name":"cartography-task-malformed",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/1",
        "cartography":"http://localhost/api/cartographies/8",
        "task":"http://localhost/api/tasks/1",
        "active": true
        }
        """;

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(false))
            .andReturn();

    Integer id =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Integer.class);
    mvc.perform(delete(TREE_NODE_URI, id)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("PATCH: active is cleared on cartography+task malformed leaf")
  @WithMockUser(roles = "ADMIN")
  void patchActiveClearedOnCartographyTaskMalformedLeaf() throws Exception {
    String createContent =
        """
        {
        "name":"cartography-task-malformed",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/1",
        "cartography":"http://localhost/api/cartographies/8",
        "task":"http://localhost/api/tasks/1"
        }
        """;

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(createContent))
            .andExpect(status().isCreated())
            .andReturn();
    Integer id =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Integer.class);

    String patchContent =
        """
        {
        "active": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, id).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mvc.perform(delete(TREE_NODE_URI, id)).andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void radioOnLeafIsRejected() throws Exception {
    String content =
        """
        {
        "name":"radio-leaf",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/7",
        "cartography":"http://localhost/api/cartographies/8",
        "radio": true
        }
        """;

    mvc.perform(post(TREE_NODES_URI).content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_SCOPE));
  }

  @Test
  @DisplayName("POST: folder child under radio parent is rejected")
  @WithMockUser(roles = "ADMIN")
  void folderChildUnderRadioParentIsRejected() throws Exception {
    String content =
        """
        {
        "name":"radio-folder-child",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/7"
        }
        """;

    mvc.perform(post(TREE_NODES_URI).content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_STRUCTURE));
  }

  @Test
  @DisplayName("POST: task child under radio parent is rejected")
  @WithMockUser(roles = "ADMIN")
  void taskChildUnderRadioParentIsRejected() throws Exception {
    String content =
        """
        {
        "name":"radio-task-child",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/7",
        "task":"http://localhost/api/tasks/1"
        }
        """;

    mvc.perform(post(TREE_NODES_URI).content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_STRUCTURE));
  }

  @Test
  @DisplayName("PATCH: enabling radio on folder with task child is rejected")
  @WithMockUser(roles = "ADMIN")
  void enablingRadioOnFolderWithTaskChildIsRejected() throws Exception {
    String createFolder =
        """
        {
        "name":"radio-task-parent",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/1"
        }
        """;

    MvcResult folderResult =
        mvc.perform(post(TREE_NODES_URI).content(createFolder))
            .andExpect(status().isCreated())
            .andReturn();
    Integer folderId =
        JsonPath.parse(folderResult.getResponse().getContentAsString()).read("$.id", Integer.class);

    String createTaskChild =
        """
        {
        "name":"radio-task-child",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/%d",
        "task":"http://localhost/api/tasks/1"
        }
        """
            .formatted(folderId);

    MvcResult taskResult =
        mvc.perform(post(TREE_NODES_URI).content(createTaskChild))
            .andExpect(status().isCreated())
            .andReturn();
    Integer taskChildId =
        JsonPath.parse(taskResult.getResponse().getContentAsString()).read("$.id", Integer.class);

    String patchRadio =
        """
        {
        "radio": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, folderId).content(patchRadio))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_STRUCTURE));

    mvc.perform(delete(TREE_NODE_URI, taskChildId)).andExpect(status().isNoContent());
    mvc.perform(delete(TREE_NODE_URI, folderId)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("POST: radio on non-cartography tree is rejected")
  @WithMockUser(roles = "ADMIN")
  void radioOnNonCartographyTreeIsRejected() throws Exception {
    String content =
        """
        {
        "name":"touristic-radio-folder",
        "tree":"http://localhost/api/trees/4",
        "radio": true
        }
        """;

    mvc.perform(post(TREE_NODES_URI).content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_SCOPE));
  }

  @Test
  @DisplayName("PATCH: two active siblings under non-radio parent are accepted")
  @WithMockUser(roles = "ADMIN")
  void twoActiveSiblingsUnderNonRadioParentAccepted() throws Exception {
    String visiblePatch =
        """
        {
        "visible": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 3).content(visiblePatch)).andExpect(status().isOk());
    mvc.perform(patch(TREE_NODE_URI, 4).content(visiblePatch)).andExpect(status().isOk());

    String patchContent =
        """
        {
        "active": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 3).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));

    mvc.perform(patch(TREE_NODE_URI, 4).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  @DisplayName("PATCH: radio folder round-trips through projection=view")
  @WithMockUser(roles = "ADMIN")
  void radioFolderRoundTripsThroughProjection() throws Exception {
    String patchContent =
        """
        {
        "radio": false
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 7).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.radio").value(false));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 7))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.radio").value(false));

    String enableRadio =
        """
        {
        "radio": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 7).content(enableRadio))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.radio").value(true));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 7))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.radio").value(true));
  }

  @Test
  @DisplayName("PATCH: loadData folder round-trips through projection=view")
  @WithMockUser(roles = "ADMIN")
  void loadDataRoundTripsThroughProjection() throws Exception {
    String enableLoadData =
        """
        {
        "loadData": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 1).content(enableLoadData))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loadData").value(true));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loadData").value(true));
  }

  @Test
  @DisplayName("PATCH: radio and loadData both round-trip on the same folder")
  @WithMockUser(roles = "ADMIN")
  void radioAndLoadDataBothRoundTrip() throws Exception {
    String patchContent =
        """
        {
        "radio": true,
        "loadData": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 7).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.radio").value(true))
        .andExpect(jsonPath("$.loadData").value(true));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 7))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.radio").value(true))
        .andExpect(jsonPath("$.loadData").value(true));
  }

  @Test
  @DisplayName("PATCH: loadData true on leaf is cleared to false")
  @WithMockUser(roles = "ADMIN")
  void loadDataOnLeafIsCleared() throws Exception {
    String patchContent =
        """
        {
        "loadData": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 8).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loadData").value(false));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 8))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loadData").value(false));
  }

  @Test
  @DisplayName("PATCH: visible false with active true clears active on existing leaf")
  @WithMockUser(roles = "ADMIN")
  void patchVisibleFalseClearsActiveOnExistingLeaf() throws Exception {
    String patchContent =
        """
        {
        "visible": false,
        "active": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 3).content(patchContent))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.visible").value(false));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 3))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false))
        .andExpect(jsonPath("$.visible").value(false));
  }

  @Test
  @DisplayName("PATCH: enabling radio on folder with direct subfolder is rejected")
  @WithMockUser(roles = "ADMIN")
  void enablingRadioOnFolderWithDirectSubfolderIsRejected() throws Exception {
    String createParentFolder =
        """
        {
        "name":"radio-subfolder-parent",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/1"
        }
        """;

    MvcResult parentResult =
        mvc.perform(post(TREE_NODES_URI).content(createParentFolder))
            .andExpect(status().isCreated())
            .andReturn();
    Integer parentId =
        JsonPath.parse(parentResult.getResponse().getContentAsString()).read("$.id", Integer.class);

    String createSubfolder =
        """
        {
        "name":"radio-subfolder-child",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/%d"
        }
        """
            .formatted(parentId);

    MvcResult subfolderResult =
        mvc.perform(post(TREE_NODES_URI).content(createSubfolder))
            .andExpect(status().isCreated())
            .andReturn();
    Integer subfolderId =
        JsonPath.parse(subfolderResult.getResponse().getContentAsString())
            .read("$.id", Integer.class);

    String patchRadio =
        """
        {
        "radio": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, parentId).content(patchRadio))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_STRUCTURE));

    mvc.perform(delete(TREE_NODE_URI, subfolderId)).andExpect(status().isNoContent());
    mvc.perform(delete(TREE_NODE_URI, parentId)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("GET: seed fixture preserves radio folder and default leaf state")
  @WithMockUser(roles = "ADMIN")
  void seedFixturePreservesRadioAndDefaultLeafState() throws Exception {
    mvc.perform(get(TREE_NODE_URI_PROJECTION, 7))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.radio").value(true))
        .andExpect(jsonPath("$.loadData").value(true));

    mvc.perform(get(TREE_NODE_URI_PROJECTION, 9))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  @DisplayName("PATCH: second active sibling under radio parent is rejected")
  @WithMockUser(roles = "ADMIN")
  void secondActiveSiblingUnderRadioParentIsRejected() throws Exception {
    String patchContent =
        """
        {
        "active": true
        }
        """;

    mvc.perform(patch(TREE_NODE_URI, 8).content(patchContent))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_DEFAULT_CONFLICT));
  }

  @Test
  @DisplayName("PUT /parent: moving folder node under radio parent via link is rejected")
  @WithMockUser(roles = "ADMIN")
  void linkSaveParentFolderUnderRadioRejected() throws Exception {
    // Node 5 is a folder (no cartography); node 7 is a radio folder.
    mvc.perform(
            put(TREE_NODE_PARENT_URI, 5)
                .contentType("text/uri-list")
                .content("http://localhost/api/tree-nodes/7"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_STRUCTURE));

    mvc.perform(get(TREE_NODE_PARENT_URI, 5))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  @DisplayName(
      "PUT /task: linking task to active cartography leaf (not under radio) via link clears active")
  @WithMockUser(roles = "ADMIN")
  void linkSaveTaskClearsActiveOnCartographyLeaf() throws Exception {
    String createContent =
        """
        {
        "name":"active-cartography-leaf",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/1",
        "cartography":"http://localhost/api/cartographies/8",
        "order": 98,
        "active": true
        }
        """;

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(createContent))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();
    Integer id =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Integer.class);

    mvc.perform(
            put(TREE_NODE_URI + "/task", id)
                .contentType("text/uri-list")
                .content("http://localhost/api/tasks/1"))
        .andExpect(status().isNoContent());

    mvc.perform(get(TREE_NODE_URI_PROJECTION, id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mvc.perform(delete(TREE_NODE_URI, id)).andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("PUT /task: linking task to cartography leaf under radio parent is rejected")
  @WithMockUser(roles = "ADMIN")
  void linkSaveTaskOnRadioChildRejected() throws Exception {
    // Node 8 is a cartography leaf under radio folder 7; linking a task violates radio structure.
    mvc.perform(
            put(TREE_NODE_URI + "/task", 8)
                .contentType("text/uri-list")
                .content("http://localhost/api/tasks/1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_STRUCTURE));
  }

  @Test
  @DisplayName("PUT /parent: moving active leaf under radio parent with active sibling is rejected")
  @WithMockUser(roles = "ADMIN")
  void linkSaveParentActiveLeafUnderRadioWithActiveSiblingRejected() throws Exception {
    String createContent =
        """
        {
        "name":"active-leaf",
        "tree":"http://localhost/api/trees/1",
        "parent":"http://localhost/api/tree-nodes/1",
        "cartography":"http://localhost/api/cartographies/8",
        "order": 99,
        "active": true
        }
        """;

    MvcResult result =
        mvc.perform(post(TREE_NODES_URI).content(createContent))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.active").value(true))
            .andReturn();
    Integer id =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Integer.class);

    mvc.perform(
            put(TREE_NODE_PARENT_URI, id)
                .contentType("text/uri-list")
                .content("http://localhost/api/tree-nodes/7"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value(ProblemTypes.TREE_NODE_RADIO_DEFAULT_CONFLICT));

    mvc.perform(delete(TREE_NODE_URI, id)).andExpect(status().isNoContent());
  }

  private void validateSizeOfResponseImage(String response, int expectedWidth, int expectedHeight) {
    String data = JsonPath.parse(response).read("$.image", String.class);
    ImageDataUri dataUri = ImageDataUri.parse(data);
    assertNotNull(dataUri);
    byte[] imageBytes = Base64.getDecoder().decode(dataUri.getData());
    try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
      BufferedImage image = ImageIO.read(bais);
      if (image == null) {
        throw new IOException("Failed to decode image");
      }
      assertEquals(expectedWidth, image.getWidth());
      assertEquals(expectedHeight, image.getHeight());
    } catch (IOException e) {
      fail("Failed to decode image: " + e.getMessage());
    }
  }
}
