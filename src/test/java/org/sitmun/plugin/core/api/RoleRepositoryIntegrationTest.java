package org.sitmun.plugin.core.api;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.plugin.core.domain.Role;
import org.sitmun.plugin.core.repository.RoleRepository;
import org.sitmun.plugin.core.test.ClientHttpLoggerRequestInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
public class RoleRepositoryIntegrationTest {

  @Autowired
  RoleRepository roleRepository;
  private RestTemplate restTemplate;
  private ArrayList<Role> roles;
  @LocalServerPort
  private int port;

  @BeforeEach
  public void setup() {
    ClientHttpRequestFactory factory =
      new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
    restTemplate = new RestTemplate(factory);
    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    if (CollectionUtils.isEmpty(interceptors)) {
      interceptors = new ArrayList<>();
    }
    interceptors.add(new ClientHttpLoggerRequestInterceptor());
    restTemplate.setInterceptors(interceptors);

    withMockSitmunAdmin(() -> {
      roles = new ArrayList<>();
      roles.add(roleRepository
        .save(Role.builder().name("RoleRepositoryTest_1").build()));
      roles.add(roleRepository
        .save(Role.builder().name("RoleRepositoryTest_2").build()));
    });
  }

  @AfterEach
  public void cleanup() {
    withMockSitmunAdmin(() -> roleRepository.deleteAll(roles));
  }

  @Test
  public void requestRoles() {
    ResponseEntity<String> response =
      restTemplate.getForEntity("http://localhost:" + port + "/api/roles", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext context = JsonPath.parse(response.getBody());

    assertThat(context.read("$._embedded.roles[*].id", JSONArray.class))
      .containsAll(
        roles.stream().map(Role::getId).collect(Collectors.toList()));
    assertThat(context.read("$._embedded.roles[*].name", JSONArray.class))
      .containsAll(
        roles.stream().map(Role::getName).collect(Collectors.toList()));
  }
}