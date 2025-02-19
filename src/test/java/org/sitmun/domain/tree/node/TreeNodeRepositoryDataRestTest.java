package org.sitmun.domain.tree.node;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tree Node Repository Data REST test")
class TreeNodeRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  @DisplayName("Retrieve tree name from node")
  void retrieveTreeName() throws Exception {
    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.treeName").value("Provincial"));
  }

  @Test
  @DisplayName("Retrieve folder")
  void retrieveFolder() throws Exception {
    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(true));
    mvc.perform(get(URIConstants.TREE_NODE_CARTOGRAPHY_URI, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Retrieve leaf")
  void retrieveLeaf() throws Exception {
    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 3)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(false));
    mvc.perform(get(URIConstants.TREE_NODE_CARTOGRAPHY_URI, 3)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Retrieve nodes from tree")
  void retrieveNodesFromTree() throws Exception {
    mvc.perform(get(URIConstants.TREE_ALL_NODES_URI, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tree-nodes", hasSize(9)))
      .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == true)]", hasSize(4)))
      .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == false)]", hasSize(5)));
  }

  @Test
  @DisplayName("New nodes can be posted")
  void newTreeNodesCanBePosted() throws Exception {
    String content = "{\"name\":\"test\",\"tree\":\"http://localhost/api/trees/1\"}";

    MvcResult result = mvc.perform(
        post(URIConstants.TREE_NODES_URI)
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(URIConstants.TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());


    mvc.perform(delete(URIConstants.TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isNoContent())
      .andReturn();
  }

  @Test
  @DisplayName("New nodes with parent can be posted")
  void newTreeNodesWithParentCanBePosted() throws Exception {
    String content = "{\"name\":\"test\",\"tree\":\"http://localhost/api/trees/1\",\"parent\":\"http://localhost/api/tree-nodes/1\"}";

    MvcResult result = mvc.perform(
        post(URIConstants.TREE_NODES_URI)
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(URIConstants.TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());

    mvc.perform(get(URIConstants.TREE_NODE_PARENT_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());

    mvc.perform(delete(URIConstants.TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isNoContent())
      .andReturn();
  }

  @Test
  @DisplayName("New nodes with image data can be posted")
  void newTreeNodesWithImageDataCanBePosted() throws Exception {
    String content = "{\"name\":\"test\",\"tree\":\"http://localhost/api/trees/1\", \"image\":\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABaElEQVR42mNk\"}";

    MvcResult result = mvc.perform(
        post(URIConstants.TREE_NODES_URI)
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.image").value("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAABaElEQVR42mNk"))
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(URIConstants.TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());


    mvc.perform(delete(URIConstants.TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isNoContent())
      .andReturn();
  }

  @Test
  @DisplayName("New nodes with image can be posted")
  void newTreeNodesWithImageUriCanBePosted() throws Exception {
    String content = "{\"name\":\"test\",\"tree\":\"http://localhost/api/trees/1\", \"image\":\"https://avatars.githubusercontent.com/u/24718368?s=96&v=4\"}";

    MvcResult result = mvc.perform(
        post(URIConstants.TREE_NODES_URI)
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andDo(MockMvcResultHandlers.print())
      .andExpect(jsonPath("$.image").value(startsWith("data:image/png;base64,iVBOR")))
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(URIConstants.TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());


    mvc.perform(delete(URIConstants.TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isNoContent())
      .andReturn();
  }

  @Test
  @DisplayName("New nodes with image with extension can be posted")
  void newTreeNodesWithImageWithExtensionUriCanBePosted() throws Exception {
    String content = "{\"name\":\"test\",\"tree\":\"http://localhost/api/trees/1\", \"image\":\"https://raw.githubusercontent.com/sitmun/community/master/logotip%20SITMUN%20JPG/horitzontal/01.principal-horit-normal.jpg\"}";

    MvcResult result = mvc.perform(
        post(URIConstants.TREE_NODES_URI)
          .content(content)
          .with(user(Fixtures.admin()))
      ).andExpect(status().isCreated())
      .andDo(MockMvcResultHandlers.print())
      .andExpect(jsonPath("$.image").value(startsWith("data:image/jpeg;base64,/9j/4AAQSkZJRg")))
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(URIConstants.TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());


    mvc.perform(delete(URIConstants.TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class))
        .with(user(Fixtures.admin()))
      )
      .andExpect(status().isNoContent())
      .andReturn();
  }

}
