package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sitmun.test.URIConstants.APPLICATIONS_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Application Default Values REST Test")
@WithMockUser(roles = "ADMIN")
class ApplicationDefaultValuesRestTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ApplicationRepository applicationRepository;

  private Application testApplication;
  private List<Application> createdApplications;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    testApplication =
        Application.builder()
            .name("Test Application for REST")
            .type("I")
            .title("Test Application Title")
            .build();
    createdApplications = new ArrayList<>();
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  void tearDown() {
    createdApplications.forEach(
        app -> {
          if (app.getId() != null) {
            applicationRepository.deleteById(app.getId());
          }
        });

    if (testApplication.getId() != null
        && applicationRepository.existsById(testApplication.getId())) {
      applicationRepository.deleteById(testApplication.getId());
    }
  }

  /**
   * Helper method to create an application via REST API and track it for cleanup.
   *
   * @param applicationJson JSON string representing the application to create
   * @return The created application if successful, null otherwise
   */
  private Application createApplicationViaRestApi(String applicationJson) throws Exception {
    String response =
        mockMvc
            .perform(
                post(APPLICATIONS_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(applicationJson))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    JsonNode responseNode = objectMapper.readTree(response);
    Integer appId = responseNode.get("id").asInt();
    Application createdApp = applicationRepository.findById(appId).orElseThrow();
    createdApplications.add(createdApp);
    return createdApp;
  }

  /**
   * Helper method to create and track an application that's saved directly to the repository.
   *
   * @param application The application to save
   * @return The saved application
   */
  private Application saveAndTrackApplication(Application application) {
    Application savedApp = applicationRepository.save(application);
    createdApplications.add(savedApp);
    return savedApp;
  }

  /**
   * Helper method to create a basic application JSON string.
   *
   * @param name Application name
   * @param type Application type
   * @param title Application title
   * @return JSON string for the application
   */
  private String createBasicApplicationJson(String name, String type, String title) {
    return """
        {
          "name": "%s",
          "type": "%s",
          "title": "%s"
        }
        """
        .formatted(name, type, title);
  }

  /**
   * Helper method to create an application JSON string with custom header parameters.
   *
   * @param name Application name
   * @param type Application type
   * @param title Application title
   * @param headerParams Custom header parameters (can be null or empty)
   * @return JSON string for the application
   */
  private String createApplicationJsonWithHeaders(
      String name, String type, String title, Map<String, Object> headerParams) {
    StringBuilder json = new StringBuilder();
    json.append(
        """
        {
          "name": "%s",
          "type": "%s",
          "title": "%s"
        """
            .formatted(name, type, title));

    if (headerParams != null) {
      try {
        String headerParamsJson = objectMapper.writeValueAsString(headerParams);
        json.append(",\n  \"headerParams\": ").append(headerParamsJson);
      } catch (Exception e) {
        // Fallback to null if serialization fails
        json.append(",\n  \"headerParams\": null");
      }
    }

    json.append("\n}");
    return json.toString();
  }

  @Test
  @DisplayName("POST application stores default booleans, dates, and header sections")
  void createsApplicationWithDefaultValuesViaRestApi() throws Exception {
    // Given
    String applicationJson =
        createBasicApplicationJson("Test Application REST", "I", "Test Application Title");

    // When
    Application createdApp = createApplicationViaRestApi(applicationJson);

    // Then
    assertThat(createdApp).isNotNull();

    mockMvc
        .perform(get(APPLICATIONS_URI + "/" + createdApp.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.treeAutoRefresh").value(true))
        .andExpect(jsonPath("$.accessParentTerritory").value(false))
        .andExpect(jsonPath("$.accessChildrenTerritory").value(false))
        .andExpect(jsonPath("$.isUnavailable").value(false))
        .andExpect(jsonPath("$.appPrivate").value(false))
        .andExpect(jsonPath("$.createdDate").exists())
        .andExpect(jsonPath("$.lastUpdate").exists())
        .andExpect(jsonPath("$.headerParams").exists())
        .andExpect(jsonPath("$.headerParams.headerLeftSection").exists())
        .andExpect(jsonPath("$.headerParams.headerRightSection").exists());
  }

  @Test
  @DisplayName("GET application returns the default header section flags")
  void returnsDefaultHeaderParametersStructureViaRestApi() throws Exception {
    // Given
    testApplication = saveAndTrackApplication(testApplication);

    // When & Then
    mockMvc
        .perform(get(APPLICATIONS_URI + "/" + testApplication.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerParams.headerLeftSection.logoSitmun.visible").value(true))
        .andExpect(
            jsonPath("$.headerParams.headerRightSection.switchApplication.visible").value(true))
        .andExpect(jsonPath("$.headerParams.headerRightSection.homeMenu.visible").value(true))
        .andExpect(jsonPath("$.headerParams.headerRightSection.switchLanguage.visible").value(true))
        .andExpect(jsonPath("$.headerParams.headerRightSection.profileButton.visible").value(true))
        .andExpect(jsonPath("$.headerParams.headerRightSection.logoutButton.visible").value(true));
  }

  @Test
  @DisplayName("POST application stores a custom header map")
  void createsApplicationWithCustomHeaderParametersViaRestApi() throws Exception {
    // Given
    Map<String, Object> customHeaders = new HashMap<>();
    customHeaders.put("customKey", "customValue");
    customHeaders.put("numericValue", 42);
    customHeaders.put("booleanValue", true);

    Map<String, Object> nestedObject = new HashMap<>();
    nestedObject.put("nestedKey", "nestedValue");
    customHeaders.put("nestedObject", nestedObject);

    String applicationJson =
        createApplicationJsonWithHeaders(
            "Test Application with Custom Headers", "I", "Test Application Title", customHeaders);

    // When
    Application createdApp = createApplicationViaRestApi(applicationJson);

    // Then
    assertThat(createdApp).isNotNull();

    mockMvc
        .perform(get(APPLICATIONS_URI + "/" + createdApp.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerParams.customKey").value("customValue"))
        .andExpect(jsonPath("$.headerParams.numericValue").value(42))
        .andExpect(jsonPath("$.headerParams.booleanValue").value(true))
        .andExpect(jsonPath("$.headerParams.nestedObject.nestedKey").value("nestedValue"));
  }

  @Test
  @DisplayName("An empty header map stays empty")
  void handlesEmptyHeaderParametersInRestApi() throws Exception {
    // Given
    String applicationJson =
        createApplicationJsonWithHeaders(
            "Test Application with Empty Headers", "I", "Test Application Title", new HashMap<>());

    // When
    Application createdApp = createApplicationViaRestApi(applicationJson);

    // Then
    assertThat(createdApp).isNotNull();

    mockMvc
        .perform(get(APPLICATIONS_URI + "/" + createdApp.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerParams").exists())
        .andExpect(jsonPath("$.headerParams").isEmpty());
  }

  @Test
  @DisplayName("Application list includes headerParams")
  void returnsApplicationsListWithDefaultValues() throws Exception {
    // Given
    testApplication = saveAndTrackApplication(testApplication);

    // When & Then
    mockMvc
        .perform(get(APPLICATIONS_URI))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.applications").exists())
        .andExpect(jsonPath("$._embedded.applications[*].headerParams").exists());
  }

  @Test
  @DisplayName("POST application stores a three-level header map")
  void preservesComplexNestedHeaderParametersStructure() throws Exception {
    // Given
    Map<String, Object> complexHeaders = new HashMap<>();
    Map<String, Object> level1 = new HashMap<>();
    Map<String, Object> level2 = new HashMap<>();
    Map<String, Object> level3 = new HashMap<>();

    level3.put("deepValue", "very deep");
    level3.put("arrayValue", new Object[] {1, 2, 3, 4, 5});

    Map<String, Object> mixedTypes = new HashMap<>();
    mixedTypes.put("string", "text");
    mixedTypes.put("number", 123.45);
    mixedTypes.put("boolean", false);
    mixedTypes.put("nullValue", null);
    level3.put("mixedTypes", mixedTypes);

    level2.put("level3", level3);
    level1.put("level2", level2);
    complexHeaders.put("level1", level1);

    String applicationJson =
        createApplicationJsonWithHeaders(
            "Test Application with Complex Headers", "I", "Test Application Title", complexHeaders);

    // When
    Application createdApp = createApplicationViaRestApi(applicationJson);

    // Then
    assertThat(createdApp).isNotNull();

    mockMvc
        .perform(get(APPLICATIONS_URI + "/" + createdApp.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerParams.level1.level2.level3.deepValue").value("very deep"))
        .andExpect(jsonPath("$.headerParams.level1.level2.level3.arrayValue").isArray())
        .andExpect(jsonPath("$.headerParams.level1.level2.level3.arrayValue[0]").value(1))
        .andExpect(jsonPath("$.headerParams.level1.level2.level3.arrayValue[4]").value(5))
        .andExpect(jsonPath("$.headerParams.level1.level2.level3.mixedTypes.string").value("text"))
        .andExpect(jsonPath("$.headerParams.level1.level2.level3.mixedTypes.number").value(123.45))
        .andExpect(jsonPath("$.headerParams.level1.level2.level3.mixedTypes.boolean").value(false))
        .andExpect(jsonPath("$.headerParams.level1.level2.level3.mixedTypes.nullValue").isEmpty());
  }
}
