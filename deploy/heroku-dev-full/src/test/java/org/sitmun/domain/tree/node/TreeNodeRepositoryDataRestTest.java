package org.sitmun.domain.tree.node;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.sitmun.test.Fixtures;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class TreeNodeRepositoryDataRestTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void retrieveTreeName() throws Exception {
    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 5345)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.treeName").value("Mòdul consulta municipal"));
  }

  @Test
  void retrieveFolder() throws Exception {
    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 5345)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(true));
    mvc.perform(get(URIConstants.TREE_NODE_CARTOGRAPHY_URI, 5345)
        .with(user(Fixtures.admin())))
      .andExpect(status().isNotFound());
  }

  @Test
  void retrieveLeaf() throws Exception {
    mvc.perform(get(URIConstants.TREE_NODE_URI_PROJECTION, 5351)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(false));
    mvc.perform(get(URIConstants.TREE_NODE_CARTOGRAPHY_URI, 5351)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk());
  }

  @Test
  void retrieveNodesFromTree() throws Exception {
    mvc.perform(get(URIConstants.TREE_ALL_NODES_URI, 1)
        .with(user(Fixtures.admin())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tree-nodes", hasSize(489)))
      .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == true)]", hasSize(80)))
      .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == false)]", hasSize(409)));
  }

  @Test
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
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
  @Disabled("Potential freeze of active connections. @Transactional may be required in REST controllers.")
  void newTreeNodesWithParentCanBePosted() throws Exception {
    String content = "{\"name\":\"test\",\"tree\":\"http://localhost/api/trees/2\",\"parent\":\"http://localhost/api/tree-nodes/416\"}";

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


}
