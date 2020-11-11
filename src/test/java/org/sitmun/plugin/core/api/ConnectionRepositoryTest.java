package org.sitmun.plugin.core.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;


import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Connection;
import org.sitmun.plugin.core.repository.ConnectionRepository;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConnectionRepositoryTest {

  @Autowired
  ConnectionRepository connectionRepository;
  private RestTemplate restTemplate;
  private ArrayList<Connection> connections;
  @LocalServerPort
  private int port;

  @Before
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
      connections = new ArrayList<>();
      connections.add(connectionRepository
          .save(Connection.builder().setName("ConnectionRepositoryTest_1").setDriver("driver1")
              .build()));
      connections.add(connectionRepository
          .save(Connection.builder().setName("ConnectionRepositoryTest_2").setDriver("driver2")
              .build()));
    });
  }

  @After
  public void cleanup() {
    withMockSitmunAdmin(() -> connectionRepository.deleteAll(connections));
  }

  @Test
  public void requestConnections() {
    ResponseEntity<String> response =
        restTemplate.getForEntity("http://localhost:" + port + "/api/connections", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext context = JsonPath.parse(response.getBody());

    assertThat(context.read("$._embedded.connections[*].id", JSONArray.class))
        .containsAll(
            connections.stream().map(it -> it.getId().intValue()).collect(Collectors.toList()));
    assertThat(context.read("$._embedded.connections[*].name", JSONArray.class))
        .containsAll(
            connections.stream().map(Connection::getName).collect(Collectors.toList()));
  }
}