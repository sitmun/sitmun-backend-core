package org.sitmun.common.domain.role;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sitmun.common.domain.role.Role;
import org.sitmun.common.domain.role.RoleRepository;
import org.sitmun.test.ClientHttpLoggerRequestInterceptor;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

class RoleRepositoryIntegrationTest {

  @Autowired
  RoleRepository roleRepository;
  private RestTemplate restTemplate;
  private ArrayList<Role> roles;
  @LocalServerPort
  private int port;

  @BeforeEach
  void setup() {
    ClientHttpRequestFactory factory =
      new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());
    restTemplate = new RestTemplate(factory);
    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
    if (CollectionUtils.isEmpty(interceptors)) {
      interceptors = new ArrayList<>();
    }
    interceptors.add(new ClientHttpLoggerRequestInterceptor());
    restTemplate.setInterceptors(interceptors);

    TestUtils.withMockSitmunAdmin(() -> {
      roles = new ArrayList<>();
      roles.add(roleRepository
        .save(Role.builder().name("RoleRepositoryTest_1").build()));
      roles.add(roleRepository
        .save(Role.builder().name("RoleRepositoryTest_2").build()));
    });
  }

  @AfterEach
  void cleanup() {
    TestUtils.withMockSitmunAdmin(() -> roleRepository.deleteAll(roles));
  }

  @Test
  void requestRoles() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
      restTemplate
        .exchange("http://localhost:" + port + "/api/roles", HttpMethod.GET, entity, String.class);

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