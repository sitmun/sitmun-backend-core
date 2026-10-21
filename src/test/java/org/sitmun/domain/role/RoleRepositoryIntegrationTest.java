package org.sitmun.domain.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.test.URIConstants.ROLES_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RoleRepositoryIntegrationTest {

  @Autowired RoleRepository roleRepository;
  @Autowired private MockMvc mvc;
  private ArrayList<Role> roles;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void setup() {
    roles = new ArrayList<>();
    roles.add(roleRepository.save(Role.builder().name("RoleRepositoryTest_1").build()));
    roles.add(roleRepository.save(Role.builder().name("RoleRepositoryTest_2").build()));
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    roleRepository.deleteAll(roles);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void requestRoles() throws Exception {
    String body =
        mvc.perform(get(ROLES_URI))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    DocumentContext context = JsonPath.parse(body);

    assertThat(context.read("$._embedded.roles[*].id", JSONArray.class))
        .containsAll(roles.stream().map(Role::getId).collect(Collectors.toList()));
    assertThat(context.read("$._embedded.roles[*].name", JSONArray.class))
        .containsAll(roles.stream().map(Role::getName).collect(Collectors.toList()));
  }
}
