package org.sitmun.plugin.core.repository.rest;

import com.jayway.jsonpath.JsonPath;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class TreeNodeRepositoryDataRestTest {

  private static final String TREE_NODES_URI = "http://localhost/api/tree-nodes";

  private static final String TREE_NODE_URI = TREE_NODES_URI + "/{0}";

  private static final String TREE_NODE_TREE_URI = TREE_NODE_URI + "/tree";

  private static final String TREE_NODE_PARENT_URI = TREE_NODE_URI + "/parent";

  private static final String TREE_NODE_URI_PROJECTION = TREE_NODE_URI + "?projection=view";

  private static final String TREE_NODE_CARTOGRAPHY_URI = TREE_NODE_URI + "/cartography";

  private static final String TREE_ALL_NODES_URI = "http://localhost/api/trees/{0}/allNodes?projection=view";

  @Autowired
  private MockMvc mvc;

  @Test
  public void retrieveFolder() throws Exception {
    mvc.perform(get(TREE_NODE_URI_PROJECTION, 5345))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(true));
    mvc.perform(get(TREE_NODE_CARTOGRAPHY_URI, 5345))
      .andExpect(status().isNotFound());
  }

  @Test
  public void retrieveLeaf() throws Exception {
    mvc.perform(get(TREE_NODE_URI_PROJECTION, 5351))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.isFolder").value(false));
    mvc.perform(get(TREE_NODE_CARTOGRAPHY_URI, 5351))
      .andExpect(status().isOk());
  }

  @Test
  public void retrieveNodesFromTree() throws Exception {
    mvc.perform(get(TREE_ALL_NODES_URI, 1))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.tree-nodes", hasSize(489)))
      .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == true)]", hasSize(80)))
      .andExpect(jsonPath("$._embedded.tree-nodes[?(@.isFolder == false)]", hasSize(409)));
  }

  @Test
  public void newTreeNodesCanBePosted() throws Exception {
    String content = "{\"name\":\"test\",\"tree\":\"http://localhost/api/trees/1\"}";

    MvcResult result = mvc.perform(
      post(TREE_NODES_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
      .andExpect(status().isOk());


    mvc.perform(delete(TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isNoContent())
      .andReturn();
  }

  @Test
  public void newTreeNodesWithParentCanBePosted() throws Exception {
    String content = "{\"name\":\"test\",\"tree\":\"http://localhost/api/trees/2\",\"parent\":\"http://localhost/api/tree-nodes/416\"}";

    MvcResult result = mvc.perform(
      post(TREE_NODES_URI)
        .content(content)
        .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    ).andExpect(status().isCreated())
      .andExpect(jsonPath("$.name").value("test"))
      .andReturn();

    String response = result.getResponse().getContentAsString();

    mvc.perform(get(TREE_NODE_TREE_URI, JsonPath.parse(response).read("$.id", Integer.class)))
      .andExpect(status().isOk());

    mvc.perform(get(TREE_NODE_PARENT_URI, JsonPath.parse(response).read("$.id", Integer.class)))
      .andExpect(status().isOk());

    mvc.perform(delete(TREE_NODE_URI, JsonPath.parse(response).read("$.id", Integer.class))
      .with(SecurityMockMvcRequestPostProcessors.user(SITMUN_ADMIN_USERNAME))
    )
      .andExpect(status().isNoContent())
      .andReturn();
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
