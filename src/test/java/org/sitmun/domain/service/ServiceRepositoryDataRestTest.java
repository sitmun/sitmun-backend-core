package org.sitmun.domain.service;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.sitmun.test.DateTimeMatchers.isIso8601DateAndTime;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Service Repository Data REST test")
class ServiceRepositoryDataRestTest {

  @Autowired private MockMvc mvc;
  @Autowired private ServiceRepository serviceRepository;

  private MockHttpServletResponse response;
  private List<Service> services;

  @BeforeEach
  void setup() {
    response = null;
    services = new ArrayList<>();
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  @WithMockUser(roles = "ADMIN")
  void create() throws Exception {
    String content =
        """
        {
          "name": "test",
          "type": "WMS",
          "blocked": false,
          "serviceURL": "https://www.example.com"
        }""";
    response =
        mvc.perform(post(SERVICES_URI).content(content))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.createdDate", isIso8601DateAndTime()))
            .andReturn()
            .getResponse();
  }

  @AfterEach
  @WithMockUser(roles = "ADMIN")
  void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(delete(location)).andExpect(status().isNoContent());
      }
    }
    serviceRepository.deleteAll(services);
  }

  @Test
  @DisplayName("GET search/content: returns only matching services")
  @WithMockUser(roles = "ADMIN")
  void searchContentReturnsOnlyMatchingServices() throws Exception {
    saveService("WMS Mountains", "WMS");
    saveService("WFS Rivers", "WFS");

    mvc.perform(
            get(SERVICES_URI + "/search/content")
                .param("q", "Mountains")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.services", hasSize(1)))
        .andExpect(jsonPath("$._embedded.services[0].name", is("WMS Mountains")));
  }

  @Test
  @DisplayName("GET search/content: is case-insensitive")
  @WithMockUser(roles = "ADMIN")
  void searchContentIsCaseInsensitive() throws Exception {
    saveService("Case Insensitive Service", "WMS");

    mvc.perform(
            get(SERVICES_URI + "/search/content")
                .param("q", "cAsE iNsEnSiTiVe")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.services", hasSize(1)))
        .andExpect(jsonPath("$._embedded.services[0].name", is("Case Insensitive Service")));
  }

  @Test
  @DisplayName("GET search/content: searches serviceURL and type")
  @WithMockUser(roles = "ADMIN")
  void searchContentSearchesServiceURLAndType() throws Exception {
    saveService(
        "Test Service Unique", "CustomWMTSType", "https://veryunique-wms-endpoint.example.com");

    mvc.perform(
            get(SERVICES_URI + "/search/content")
                .param("q", "veryunique-wms-endpoint")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.services", hasSize(1)))
        .andExpect(jsonPath("$._embedded.services[0].name", is("Test Service Unique")));

    mvc.perform(
            get(SERVICES_URI + "/search/content")
                .param("q", "CustomWMTSType")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.services", hasSize(1)))
        .andExpect(jsonPath("$._embedded.services[0].name", is("Test Service Unique")));
  }

  @Test
  @DisplayName("GET search/content: reports filtered page totals")
  @WithMockUser(roles = "ADMIN")
  void searchContentReportsFilteredPageTotals() throws Exception {
    saveService("Paged Search One", "WMS");
    saveService("Paged Search Two", "WMS");
    saveService("Paged Other", "WMS");

    mvc.perform(
            get(SERVICES_URI + "/search/content")
                .param("q", "Paged Search")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.services", hasSize(1)))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$.page.totalPages", is(2)));
  }

  private Service saveService(String name, String type) {
    return saveService(name, type, "https://www.example.com/" + name.replace(" ", "-"));
  }

  private Service saveService(String name, String type, String serviceURL) {
    Service saved =
        serviceRepository.save(
            Service.builder().name(name).type(type).serviceURL(serviceURL).blocked(false).build());
    services.add(saved);
    return saved;
  }
}
