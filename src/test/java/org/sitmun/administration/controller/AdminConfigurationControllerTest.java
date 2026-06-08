package org.sitmun.administration.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.test.BaseTest;
import org.springframework.security.test.context.support.WithMockUser;

@DisplayName("Admin configuration controller test")
class AdminConfigurationControllerTest extends BaseTest {

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET: Returns image upload constraints for tree images")
  void returnsTreeImageUploadConstraints() throws Exception {
    mvc.perform(get("/api/config/admin"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.imageUpload.tree.supportedFormats")
                .value(containsInAnyOrder("png", "jpg", "jpeg")))
        .andExpect(jsonPath("$.imageUpload.tree.maxBytes").value(2097152))
        .andExpect(jsonPath("$.imageUpload.tree.defaultSize.width").value(125))
        .andExpect(jsonPath("$.imageUpload.tree.defaultSize.height").value(125))
        .andExpect(jsonPath("$.imageUpload.tree.sizesByType").value(hasKey("menu")))
        .andExpect(jsonPath("$.imageUpload.tree.sizesByType.menu.width").value(50))
        .andExpect(jsonPath("$.imageUpload.tree.sizesByType.menu.height").value(50))
        .andExpect(jsonPath("$.imageUpload.tree.sizesByType").value(hasKey("list")))
        .andExpect(jsonPath("$.imageUpload.tree.sizesByType.list.width").value(350))
        .andExpect(jsonPath("$.imageUpload.tree.sizesByType.list.height").value(350));
  }

  @Test
  @DisplayName("GET: Returns 401 when request has no credentials")
  void returnsUnauthorizedWithoutCredentials() throws Exception {
    mvc.perform(get("/api/config/admin")).andExpect(status().isUnauthorized());
  }
}
