package org.sitmun.domain.database;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.sitmun.authentication.dto.AuthenticationResponse;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("DatabaseConnection Repository Integration test")
class DatabaseConnectionRepositoryIntegrationTest {

  @Autowired
  DatabaseConnectionRepository connectionRepository;
  private RestTemplate restTemplate;
  private ArrayList<DatabaseConnection> connections;
  @LocalServerPort
  private int port;

  @SuppressWarnings("rawtypes")
  private static List getListOfCartographies(JSONObject cartographies) {
    return JsonPath.parse(cartographies.toString()).read("$._embedded.cartographies[*]._links.self.href", List.class);
  }

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

      connections = new ArrayList<>();
      connections.add(connectionRepository
        .save(DatabaseConnection.builder().name("ConnectionRepositoryTest_1").driver("driver1")
          .build()));
      connections.add(connectionRepository
        .save(DatabaseConnection.builder().name("ConnectionRepositoryTest_2").driver("driver2")
          .build()));
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() {
    connectionRepository.deleteAll(connections);
  }

  @Test
  @DisplayName("GET: List DatabaseConnection")
  void requestConnections() {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
      restTemplate.exchange("http://localhost:" + port + "/api/connections", HttpMethod.GET, entity, String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    DocumentContext context = JsonPath.parse(response.getBody());

    assertThat(context.read("$._embedded.connections[*].id", JSONArray.class))
      .containsAll(
        connections.stream().map(DatabaseConnection::getId).collect(Collectors.toList()));
    assertThat(context.read("$._embedded.connections[*].name", JSONArray.class))
      .containsAll(
        connections.stream().map(DatabaseConnection::getName).collect(Collectors.toList()));
  }

  @Test
  @DisplayName("PUT: Update DatabaseConnection")
  void updateConnection() throws JSONException {
    JSONObject updatedValueJson = getConnection(1);
    assertThat(updatedValueJson.get("user")).isEqualTo("User1");

    String auth = getAuthorization();

    updatedValueJson.put("user", "User3");
    updateConnection(1, updatedValueJson, auth);

    assertThat(getConnection(1).get("user")).isEqualTo("User3");

    updatedValueJson.put("user", "User1");
    updateConnection(1, updatedValueJson, auth);

    assertThat(getConnection(1).get("user")).isEqualTo("User1");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  @DisplayName("PUT: Cannot update connection for catographies")
  void putCannotUpdateConnectionCartographies() throws JSONException {
    JSONObject oldValueJson = getConnectionCartographies(1);
    List oldList = getListOfCartographies(oldValueJson);

    String auth = getAuthorization();

    List newList = Arrays.asList("http://localhost:" + port + "/api/cartographies/1",
      "http://localhost:" + port + "/api/cartographies/2",
      "http://localhost:" + port + "/api/cartographies/3",
      "http://localhost:" + port + "/api/cartographies/4",
      "http://localhost:" + port + "/api/cartographies/5",
      "http://localhost:" + port + "/api/cartographies/6");

    updateConnectionCartograhies(1, newList, auth);

    oldValueJson = getConnectionCartographies(1);
    List updatedList = getListOfCartographies(oldValueJson);

    assertThat(updatedList).containsExactlyInAnyOrderElementsOf(oldList);
  }

  @Test
  @DisplayName("PUT: Can update cartography spatial selection connection")
  @Disabled("Requires additional test data")
  void putCanUpdateCartographySpatialSelectionConnection() throws JSONException {
    String auth = getAuthorization();
    assertThat(hasCartographiesSpatialSelectionConnection(1, auth)).isTrue();
    JSONObject cartographies = getConnectionCartographies(2);
    assertThat(getListOfCartographies(cartographies)).hasSize(7);

    deleteCartographiesSpatialSelectionConnection(1, auth);
    assertThat(hasCartographiesSpatialSelectionConnection(1, auth)).isFalse();
    cartographies = getConnectionCartographies(2);
    assertThat(getListOfCartographies(cartographies)).hasSize(6);


    updateCartographiesSpatialSelectionConnection(1, Collections.singletonList("http://localhost:" + port + "/api/connections/2"), auth);
    assertThat(hasCartographiesSpatialSelectionConnection(1, auth)).isTrue();
    cartographies = getConnectionCartographies(2);
    assertThat(getListOfCartographies(cartographies)).hasSize(7);
  }

  private String getAuthorization() {
    UserPasswordAuthenticationRequest login = new UserPasswordAuthenticationRequest();
    login.setUsername("admin");
    login.setPassword("admin");
    ResponseEntity<AuthenticationResponse> loginResponse =
      restTemplate
        .postForEntity("http://localhost:{port}/api/authenticate", login, AuthenticationResponse.class, port);
    assertThat(loginResponse.getBody()).isNotNull();
    return loginResponse.getBody().getIdToken();
  }

  private JSONObject getConnection(int id) throws JSONException {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    String uri = "http://localhost:" + port + "/api/connections/" + id;
    String oldValue =
      restTemplate.exchange(uri, HttpMethod.GET, entity, String.class).getBody();
    if (oldValue == null) {
      return null;
    }
    return new JSONObject(oldValue);
  }

  private JSONObject getConnectionCartographies(int id) throws JSONException {
    HttpHeaders headers = new HttpHeaders();
    headers.set(HttpHeaders.AUTHORIZATION, TestUtils.requestAuthorization(restTemplate, port));
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    String uri = "http://localhost:" + port + "/api/connections/" + id + "/cartographies";
    String oldValue =
      restTemplate.exchange(uri, HttpMethod.GET, entity, String.class).getBody();
    if (oldValue == null) {
      return null;
    }
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
}