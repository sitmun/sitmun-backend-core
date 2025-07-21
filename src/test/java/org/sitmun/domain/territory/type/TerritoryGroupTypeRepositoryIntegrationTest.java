package org.sitmun.domain.territory.type;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.test.ClientHttpLoggerRequestInterceptor;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Deprecated
class TerritoryGroupTypeRepositoryIntegrationTest {

  @Autowired
  TerritoryGroupTypeRepository territoryGroupTypeRepository;
  private RestTemplate restTemplate;
  private ArrayList<TerritoryGroupType> territoryGroupTypes;
  @LocalServerPort
  private int port;

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
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

      territoryGroupTypes = new ArrayList<>();
      territoryGroupTypes.add(territoryGroupTypeRepository
        .save(TerritoryGroupType.builder().name("TerritoryGroupTypeTest_1").build()));
      territoryGroupTypes.add(territoryGroupTypeRepository
        .save(TerritoryGroupType.builder().name("TerritoryGroupTypeTest_2").build()));
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    territoryGroupTypeRepository.deleteAll(territoryGroupTypes);
  }

  @Test
  void requestRoles() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
      restTemplate
        .exchange("http://localhost:" + port + "/api/territory-group-types", HttpMethod.GET, entity, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext context = JsonPath.parse(response.getBody());

    assertThat(context.read("$._embedded.territory-group-types[*].id", JSONArray.class))
      .containsAll(
        territoryGroupTypes.stream().map(TerritoryGroupType::getId)
          .collect(Collectors.toList()));
    assertThat(context.read("$._embedded.territory-group-types[*].name", JSONArray.class))
      .containsAll(
        territoryGroupTypes.stream().map(TerritoryGroupType::getName)
          .collect(Collectors.toList()));
  }
}