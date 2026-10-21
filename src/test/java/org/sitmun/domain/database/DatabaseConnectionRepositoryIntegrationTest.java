package org.sitmun.domain.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.test.URIConstants.CONNECTIONS_URI;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("DatabaseConnection Repository Integration test")
class DatabaseConnectionRepositoryIntegrationTest {

  @Autowired DatabaseConnectionRepository connectionRepository;
  @Autowired private MockMvc mvc;
  private ArrayList<DatabaseConnection> connections;

  @SuppressWarnings("rawtypes")
  private static List getListOfCartographies(JSONObject cartographies) {
    return JsonPath.parse(cartographies.toString())
        .read("$._embedded.cartographies[*]._links.self.href", List.class);
  }

  @BeforeEach
  @WithMockUser(roles = "ADMIN")
  void setup() {
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
  @WithMockUser(roles = "ADMIN")
  void requestConnections() throws Exception {
    String body =
        mvc.perform(get(CONNECTIONS_URI))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    DocumentContext context = JsonPath.parse(body);

    assertThat(context.read("$._embedded.connections[*].id", JSONArray.class))
        .containsAll(
            connections.stream().map(DatabaseConnection::getId).collect(Collectors.toList()));
    assertThat(context.read("$._embedded.connections[*].name", JSONArray.class))
        .containsAll(
            connections.stream().map(DatabaseConnection::getName).collect(Collectors.toList()));
  }

  @Test
  @DisplayName("PUT: Update DatabaseConnection")
  @WithMockUser(roles = "ADMIN")
  void updateConnection() throws Exception {
    JSONObject updatedValueJson = getConnection(1);
    assertThat(Objects.requireNonNull(updatedValueJson).get("user")).isEqualTo("User1");

    updatedValueJson.put("user", "User3");
    updateConnection(1, updatedValueJson);

    assertThat(Objects.requireNonNull(getConnection(1)).get("user")).isEqualTo("User3");

    updatedValueJson.put("user", "User1");
    updateConnection(1, updatedValueJson);

    assertThat(Objects.requireNonNull(getConnection(1)).get("user")).isEqualTo("User1");
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  @DisplayName("PUT: Cannot update connection for catographies")
  @WithMockUser(roles = "ADMIN")
  void putCannotUpdateConnectionCartographies() throws Exception {
    JSONObject oldValueJson = getConnectionCartographies(1);
    List oldList = getListOfCartographies(Objects.requireNonNull(oldValueJson));

    List newList =
        Arrays.asList(
            "http://localhost/api/cartographies/1",
            "http://localhost/api/cartographies/2",
            "http://localhost/api/cartographies/3",
            "http://localhost/api/cartographies/4",
            "http://localhost/api/cartographies/5",
            "http://localhost/api/cartographies/6");

    updateConnectionCartograhies(1, newList);

    oldValueJson = getConnectionCartographies(1);
    List updatedList = getListOfCartographies(Objects.requireNonNull(oldValueJson));

    assertThat(updatedList).containsExactlyInAnyOrderElementsOf(oldList);
  }

  private JSONObject getConnection(int id) throws Exception {
    String body =
        mvc.perform(get(CONNECTIONS_URI + "/" + id))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    if (body == null || body.isEmpty()) {
      return null;
    }
    return new JSONObject(body);
  }

  private JSONObject getConnectionCartographies(int id) throws Exception {
    String body =
        mvc.perform(get(CONNECTIONS_URI + "/" + id + "/cartographies"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    if (body == null || body.isEmpty()) {
      return null;
    }
    return new JSONObject(body);
  }

  private void updateConnectionCartograhies(int id, List<String> newValue) throws Exception {
    mvc.perform(
            put(CONNECTIONS_URI + "/" + id + "/cartographies")
                .contentType("text/uri-list")
                .content(String.join("\n", newValue)))
        .andExpect(status().is2xxSuccessful());
  }

  private void updateConnection(int id, JSONObject newValue) throws Exception {
    mvc.perform(
            put(CONNECTIONS_URI + "/" + id)
                .contentType(APPLICATION_JSON)
                .content(newValue.toString()))
        .andExpect(status().is2xxSuccessful());
  }
}
