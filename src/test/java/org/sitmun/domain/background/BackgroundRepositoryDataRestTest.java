package org.sitmun.domain.background;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.sitmun.test.URIConstants.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Background Repository Data REST test")
class BackgroundRepositoryDataRestTest {

  @Autowired private MockMvc mvc;
  @Autowired private BackgroundRepository backgroundRepository;

  @Nullable private MockHttpServletResponse response;
  private List<Background> backgrounds;

  @BeforeEach
  void init() {
    backgrounds = new ArrayList<>();
  }

  @Test
  @DisplayName("POST: minimum set of properties")
  @WithMockUser(roles = "ADMIN")
  void createBackground() throws Exception {
    String content =
        """
        {
        "name":"test",
        "description":"test",
        "active":"true"
        }""";

    response =
        mvc.perform(post(BACKGROUNDS_URI).content(content))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
  }

  @Test
  @DisplayName("POST: fail creation when cartography group has invalid background map")
  @WithMockUser(roles = "ADMIN")
  void failCreateBackgroundWithInvalidMap() throws Exception {
    String content =
        """
        {
        "name":"test",
        "description":"test",
        "active":"true",
        "cartographyGroup":"http://localhost/api/cartography-group/1"
        }""";
    mvc.perform(post(BACKGROUNDS_URI).content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].rejectedValue").value("C"));
  }

  @Test
  @DisplayName("PUT: fail update of cartography group with invalid background map")
  @WithMockUser(roles = "ADMIN")
  void failUpdateBackgroundWithInvalidMap() throws Exception {
    String content = "http://localhost/api/cartography-group/1";
    mvc.perform(
            put(BACKGROUND_URI_CARTOGRAPHY_GROUP, 1).content(content).contentType("text/uri-list"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].rejectedValue").value("C"));
  }

  @AfterEach
  void cleanup() throws Exception {
    if (response != null) {
      String location = response.getHeader("Location");
      if (location != null) {
        mvc.perform(delete(location)).andExpect(status().isNoContent());
      }
      response = null;
    }
    backgroundRepository.deleteAll(backgrounds);
  }

  @Test
  @DisplayName("GET search/content: returns only matching backgrounds")
  @WithMockUser(roles = "ADMIN")
  void searchContentReturnsOnlyMatchingBackgrounds() throws Exception {
    saveBackground("Mountain Roads");
    saveBackground("Ocean View");

    mvc.perform(
            get(BACKGROUNDS_URI + "/search/content")
                .param("q", "Mountain")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.backgrounds", hasSize(1)))
        .andExpect(jsonPath("$._embedded.backgrounds[0].name", is("Mountain Roads")));
  }

  @Test
  @DisplayName("GET search/content: is case-insensitive")
  @WithMockUser(roles = "ADMIN")
  void searchContentIsCaseInsensitive() throws Exception {
    saveBackground("Case Insensitive Background");

    mvc.perform(
            get(BACKGROUNDS_URI + "/search/content")
                .param("q", "cAsE iNsEnSiTiVe")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.backgrounds", hasSize(1)))
        .andExpect(jsonPath("$._embedded.backgrounds[0].name", is("Case Insensitive Background")));
  }

  @Test
  @DisplayName("GET search/content: searches description field")
  @WithMockUser(roles = "ADMIN")
  void searchContentSearchesDescription() throws Exception {
    saveBackground("Test BG", "Unique Description Here");

    mvc.perform(
            get(BACKGROUNDS_URI + "/search/content")
                .param("q", "Unique Description")
                .param("page", "0")
                .param("size", "100")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.backgrounds", hasSize(1)))
        .andExpect(jsonPath("$._embedded.backgrounds[0].name", is("Test BG")));
  }

  @Test
  @DisplayName("GET search/content: reports filtered page totals")
  @WithMockUser(roles = "ADMIN")
  void searchContentReportsFilteredPageTotals() throws Exception {
    saveBackground("Paged Search One");
    saveBackground("Paged Search Two");
    saveBackground("Paged Other");

    mvc.perform(
            get(BACKGROUNDS_URI + "/search/content")
                .param("q", "Paged Search")
                .param("page", "0")
                .param("size", "1")
                .param("sort", "name,ASC")
                .param("sort", "id,ASC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.backgrounds", hasSize(1)))
        .andExpect(jsonPath("$.page.totalElements", is(2)))
        .andExpect(jsonPath("$.page.totalPages", is(2)));
  }

  private Background saveBackground(String name) {
    return saveBackground(name, null);
  }

  private Background saveBackground(String name, String description) {
    Background saved =
        backgroundRepository.save(
            Background.builder().name(name).description(description).active(true).build());
    backgrounds.add(saved);
    return saved;
  }
}
