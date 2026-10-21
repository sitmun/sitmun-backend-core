package org.sitmun.domain.database;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.*;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.sitmun.test.ClientHttpLoggerRequestInterceptor;
import org.sitmun.test.TestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("DatabaseConnection Repository Integration test")
class DatabaseConnectionRepositoryIntegrationTest {

  @Autowired DatabaseConnectionRepository connectionRepository;
  private RestTemplate restTemplate;
  private ArrayList<DatabaseConnection> connections;
  @LocalServerPort private int port;

  @SuppressWarnings("rawtypes")
  private static List getListOfCartographies(JSONObject cartographies) {
    return JsonPath.parse(cartographies.toString())
        .read("$._embedded.cartographies[*]._links.self.href", List.class);
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
    connections.add(
        connectionRepository.save(
            DatabaseConnection.builder()
                .name("ConnectionRepositoryTest_1")
                .driver("driver1")
                .build()));
    connections.add(
        connectionRepository.save(
            DatabaseConnection.builder()
                .name("ConnectionRepositoryTest_2")
                .driver("driver2")
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
    HttpHeaders headers = TestUtils.adminAuthHeaders(restTemplate, port);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            "http://localhost:" + port + "/api/connections", HttpMethod.GET, entity, String.class);
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
    assertThat(Objects.requireNonNull(updatedValueJson).get("user")).isEqualTo("User1");

    HttpHeaders auth = getAuthorization();

    updatedValueJson.put("user", "User3");
    updateConnection(1, updatedValueJson, auth);

    assertThat(Objects.requireNonNull(getConnection(1)).get("user")).isEqualTo("User3");

    updatedValueJson.put("user", "User1");
    updateConnection(1, updatedValueJson, auth);

    assertThat(Objects.requireNonNull(getConnection(1)).get("user")).isEqualTo("User1");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  @DisplayName("PUT: Cannot update connection for catographies")
  void putCannotUpdateConnectionCartographies() throws JSONException {
    JSONObject oldValueJson = getConnectionCartographies(1);
    List oldList = getListOfCartographies(Objects.requireNonNull(oldValueJson));

    HttpHeaders auth = getAuthorization();

    List newList =
        Arrays.asList(
            "http://localhost:" + port + "/api/cartographies/1",
            "http://localhost:" + port + "/api/cartographies/2",
            "http://localhost:" + port + "/api/cartographies/3",
            "http://localhost:" + port + "/api/cartographies/4",
            "http://localhost:" + port + "/api/cartographies/5",
            "http://localhost:" + port + "/api/cartographies/6");

    updateConnectionCartograhies(1, newList, auth);

    oldValueJson = getConnectionCartographies(1);
    List updatedList = getListOfCartographies(Objects.requireNonNull(oldValueJson));

    assertThat(updatedList).containsExactlyInAnyOrderElementsOf(oldList);
  }

  private HttpHeaders getAuthorization() {
    return TestUtils.adminAuthHeaders(restTemplate, port);
  }

  private JSONObject getConnection(int id) throws JSONException {
    HttpHeaders headers = getAuthorization();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    String uri = "http://localhost:" + port + "/api/connections/" + id;
    String oldValue = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class).getBody();
    if (oldValue == null) {
      return null;
    }
    return new JSONObject(oldValue);
  }

  private JSONObject getConnectionCartographies(int id) throws JSONException {
    HttpHeaders headers = getAuthorization();
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    String uri = "http://localhost:" + port + "/api/connections/" + id + "/cartographies";
    String oldValue = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class).getBody();
    if (oldValue == null) {
      return null;
    }
    return new JSONObject(oldValue);
  }

  private void updateConnectionCartograhies(int id, List<String> newValue, HttpHeaders auth) {
    HttpHeaders headers = new HttpHeaders(auth);
    headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/uri-list"));
    restTemplate.exchange(
        "http://localhost:" + port + "/api/connections/" + id + "/cartographies",
        HttpMethod.PUT,
        new HttpEntity<>(String.join("\n", newValue), headers),
        String.class);
  }

  private void updateConnection(int id, JSONObject newValue, HttpHeaders auth) {
    HttpHeaders headers = new HttpHeaders(auth);
    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
    restTemplate.exchange(
        "http://localhost:" + port + "/api/connections/" + id,
        HttpMethod.PUT,
        new HttpEntity<>(newValue.toString(), headers),
        String.class);
  }
}
