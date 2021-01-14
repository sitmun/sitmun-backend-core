package org.sitmun.plugin.core.repository.rest;

import com.jayway.jsonpath.JsonPath;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.config.RepositoryRestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class RepositoryDataRestUpdateTest {

  private static final String SITUATION_MAPS_URI =
    "http://localhost/api/situation-maps";

  private static final String SITUATION_MAP_URI = SITUATION_MAPS_URI + "/{0}";

  private static final String SITUATION_MAP_ROLES_URI = SITUATION_MAP_URI + "/roles";

  @Autowired
  private MockMvc mvc;

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void itemResourceUpdate() throws Exception {
    String item = mvc.perform(get(SITUATION_MAP_URI, 132))
      .andDo(print())
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    JSONObject json = new JSONObject(item);
    String oldName = json.getString("name");
    json.put("name", "New name");

    mvc.perform(put(SITUATION_MAP_URI, 132).content(json.toString()))
      .andExpect(status().isOk());

    mvc.perform(get(SITUATION_MAP_URI, 132))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value("New name"));

    json.put("name", oldName);

    mvc.perform(put(SITUATION_MAP_URI, 132).content(json.toString()))
      .andExpect(status().isOk());

    mvc.perform(get(SITUATION_MAP_URI, 132))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.name").value(oldName));
  }

  @Test
  @WithMockUser(username = SITMUN_ADMIN_USERNAME)
  public void associationResourceUpdate() throws Exception {
    String item = mvc.perform(get(SITUATION_MAP_ROLES_URI, 132))
      .andDo(print())
      .andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString();

    @SuppressWarnings("unchecked")
    List<String> links = (List<String>) JsonPath.parse(item).read("$._embedded.roles[*]._links.self.href", List.class);

    String update = links.stream().skip(1).collect(Collectors.joining("\n"));

    mvc.perform(put(SITUATION_MAP_ROLES_URI, 132).content(update).contentType("text/uri-list"))
      .andDo(print());

    mvc.perform(get(SITUATION_MAP_ROLES_URI, 132))
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", hasSize(links.size() - 1)));

    update = String.join("\n", links);

    mvc.perform(put(SITUATION_MAP_ROLES_URI, 132).content(update).contentType("text/uri-list"))
      .andDo(print());

    mvc.perform(get(SITUATION_MAP_ROLES_URI, 132))
      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", hasSize(links.size())))
      .andExpect(jsonPath("$._embedded.roles[*]._links.self.href", containsInAnyOrder(links.toArray())));
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
