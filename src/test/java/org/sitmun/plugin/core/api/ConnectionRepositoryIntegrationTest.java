package org.sitmun.plugin.core.api;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sitmun.plugin.core.domain.Connection;
import org.sitmun.plugin.core.repository.ConnectionRepository;
import org.sitmun.plugin.core.test.ClientHttpLoggerRequestInterceptor;
import org.sitmun.plugin.core.web.rest.AuthController;
import org.sitmun.plugin.core.web.rest.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_PASSWORD;
import static org.sitmun.plugin.core.test.TestConstants.SITMUN_ADMIN_USERNAME;
import static org.sitmun.plugin.core.test.TestUtils.withMockSitmunAdmin;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConnectionRepositoryIntegrationTest {

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
        connections.stream().map(Connection::getId).collect(Collectors.toList()));
    assertThat(context.read("$._embedded.connections[*].name", JSONArray.class))
      .containsAll(
        connections.stream().map(Connection::getName).collect(Collectors.toList()));
  }

  @Test
  public void updateConnection() throws JSONException {
    JSONObject updatedValueJson = getConnection(2);
    assertThat(updatedValueJson.get("user")).isEqualTo("User2");

    String auth = getAuthorization();

    updatedValueJson.put("user", "User3");
    updateConnection(2, updatedValueJson, auth);

    assertThat(getConnection(2).get("user")).isEqualTo("User3");

    updatedValueJson.put("user", "User2");
    updateConnection(2, updatedValueJson, auth);

    assertThat(getConnection(2).get("user")).isEqualTo("User2");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  public void putCannotUpdateConnectionCartographies() throws JSONException {
    JSONObject oldValueJson = getConnectionCartographies(2);
    List oldList = getListOfCartographies(oldValueJson);

    String auth = getAuthorization();

    List newList = Arrays.asList("http://localhost:" + port + "/api/cartographies/1255",
      "http://localhost:" + port + "/api/cartographies/1055",
      "http://localhost:" + port + "/api/cartographies/1086",
      "http://localhost:" + port + "/api/cartographies/1043",
      "http://localhost:" + port + "/api/cartographies/1254",
      "http://localhost:" + port + "/api/cartographies/154");

    updateConnectionCartograhies(2, newList, auth);

    oldValueJson = getConnectionCartographies(2);
    List updatedList = getListOfCartographies(oldValueJson);

    assertThat(updatedList).containsExactlyInAnyOrderElementsOf(oldList);
  }

  @Test
  public void putCanUpdateCartographySpatialSelectionConnection() throws JSONException {
    String auth = getAuthorization();
    assertThat(hasCartographiesSpatialSelectionConnection(1255, auth)).isTrue();
    JSONObject cartographies = getConnectionCartographies(2);
    assertThat(getListOfCartographies(cartographies)).hasSize(7);

    deleteCartographiesSpatialSelectionConnection(1255, auth);
    assertThat(hasCartographiesSpatialSelectionConnection(1255, auth)).isFalse();
    cartographies = getConnectionCartographies(2);
    assertThat(getListOfCartographies(cartographies)).hasSize(6);


    updateCartographiesSpatialSelectionConnection(1255, Collections.singletonList("http://localhost:" + port + "/api/connections/2"), auth);
    assertThat(hasCartographiesSpatialSelectionConnection(1255, auth)).isTrue();
    cartographies = getConnectionCartographies(2);
    assertThat(getListOfCartographies(cartographies)).hasSize(7);
  }

  private String getAuthorization() {
    LoginRequest login = new LoginRequest();
    login.setUsername(SITMUN_ADMIN_USERNAME);
    login.setPassword(SITMUN_ADMIN_PASSWORD);
    ResponseEntity<AuthController.JWTToken> loginResponse =
      restTemplate
        .postForEntity("http://localhost:{port}/api/authenticate", login, AuthController.JWTToken.class, port);
    assertThat(loginResponse.getBody()).isNotNull();
    return loginResponse.getBody().getIdToken();
  }


  private JSONObject getConnection(int id) throws JSONException {
    String uri = "http://localhost:{0}/api/connections/{1}";
    String oldValue =
      restTemplate.getForEntity(uri, String.class, port, id).getBody();
    return new JSONObject(oldValue);
  }

  private JSONObject getConnectionCartographies(int id) throws JSONException {
    String uri = "http://localhost:{0}/api/connections/{1}/cartographies";
    String oldValue =
      restTemplate.getForEntity(uri, String.class, port, id).getBody();
    return new JSONObject(oldValue);
  }

  private boolean hasCartographiesSpatialSelectionConnection(int id, String auth) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(auth);
    try {
      restTemplate.exchange("http://localhost:" + port + "/api/cartographies/" + id + "/spatialSelectionConnection",
        HttpMethod.GET,
        new HttpEntity<>(headers),
        String.class);
      return true;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return false;
      }
      throw e;
    }
  }


  private void deleteCartographiesSpatialSelectionConnection(int id, String auth) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(auth);
    restTemplate.exchange("http://localhost:" + port + "/api/cartographies/" + id + "/spatialSelectionConnection",
      HttpMethod.DELETE,
      new HttpEntity<>(headers), Void.class);
  }

  private void updateCartographiesSpatialSelectionConnection(int id, List<String> newValue, String auth) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/uri-list"));
    headers.setBearerAuth(auth);
    restTemplate.exchange("http://localhost:" + port + "/api/cartographies/" + id + "/spatialSelectionConnection",
      HttpMethod.PUT,
      new HttpEntity<>(String.join("\n", newValue), headers), String.class);
  }

  private void updateConnectionCartograhies(int id, List<String> newValue, String auth) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/uri-list"));
    headers.setBearerAuth(auth);
    restTemplate.exchange("http://localhost:" + port + "/api/connections/" + id + "/cartographies",
      HttpMethod.PUT,
      new HttpEntity<>(String.join("\n", newValue), headers), String.class);
  }

  private void updateConnection(int id, JSONObject newValue, String auth) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(auth);
    restTemplate.exchange("http://localhost:" + port + "/api/connections/" + id,
      HttpMethod.PUT,
      new HttpEntity<>(newValue.toString(), headers), String.class);
  }

  @SuppressWarnings("rawtypes")
  private List getListOfCartographies(JSONObject cartographies) {
    return JsonPath.parse(cartographies.toString()).read("$._embedded.cartographies[*]._links.self.href", List.class);
  }
}