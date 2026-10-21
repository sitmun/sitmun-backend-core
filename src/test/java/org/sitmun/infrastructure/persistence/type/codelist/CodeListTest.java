package org.sitmun.infrastructure.persistence.type.codelist;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.test.BaseTest;
import org.sitmun.test.URIConstants;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("Codelist validator test")
@Import(CodeListValidator.class)
class CodeListTest extends BaseTest {

  @MockitoBean private CodeListValueRepository codeListValueRepository;

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("An unknown legend type is rejected")
  void failIfCodeListValueIsWrong() throws Exception {

    when(codeListValueRepository.existsByCodeListNameAndValue(
            CodeListsConstants.CARTOGRAPHY_LEGEND_TYPE, "WRONG VALUE"))
        .thenReturn(false);

    String content = new JSONObject().put("legendType", "WRONG VALUE").toString();

    mvc.perform(
            post(URIConstants.CARTOGRAPHIES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
        .andExpect(status().is4xxClientError())
        .andExpect(
            jsonPath(
                "$.errors.[?(@.field == 'legendType')].rejectedValue", hasItem("WRONG VALUE")));
  }
}
