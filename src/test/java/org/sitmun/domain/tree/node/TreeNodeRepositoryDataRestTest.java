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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.infrastructure.persistence.type.image.ImageDataUri;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tree Node Repository Data REST test")
class TreeNodeRepositoryDataRestTest {

  private static final String PNG_8X8_TRANSPARENT =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAgAAAAIAQMAAAD+wSzIAAAABlBMVEX///+/v7+jQ3Y5AAAADklEQVQI12P4AIX8EAgALgAD/aNpbtEAAAAASUVORK5CYII";
  private static final String PNG_125X125_TRANSPARENT =
      "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAH0AAAB9AQAAAACn+1GIAAAAIElEQVR4XmP4jwp+MIwKjAqMCowKjAqMCowKjAqQIAAAMVDFL8q1f5EAAAAASUVORK5CYII=";

  @Autowired private MockMvc mvc;

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
        .andExpect(jsonPath("$._embedded.tree-nodes", hasSize(9)))
        .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == true)]", hasSize(4)))
        .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == false)]", hasSize(5)));
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
        }""".formatted(uniqueName);

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
