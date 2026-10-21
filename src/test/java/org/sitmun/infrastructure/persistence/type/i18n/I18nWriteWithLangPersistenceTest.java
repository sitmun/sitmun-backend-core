package org.sitmun.infrastructure.persistence.type.i18n;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("I18n write with non-default lang")
class I18nWriteWithLangPersistenceTest {

  private static final int TREE_ID = 4;

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("PUT ?lang=ca persists the submitted tree name")
  void putWithNonDefaultLangPersistsSubmittedName() throws Exception {
    String original =
        jdbcTemplate.queryForObject(
            "SELECT TRE_NAME FROM STM_TREE WHERE TRE_ID = ?", String.class, TREE_ID);
    String updated = original + "-293";
    try {
      String treeJson =
          mvc.perform(get("/api/trees/{id}", TREE_ID))
              .andExpect(status().isOk())
              .andReturn()
              .getResponse()
              .getContentAsString();
      String body = JsonPath.parse(treeJson).set("$.name", updated).jsonString();

      mvc.perform(
              put("/api/trees/{id}", TREE_ID)
                  .param("lang", "ca")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value(updated));

      mvc.perform(get("/api/trees/{id}", TREE_ID))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.name").value(updated));
    } finally {
      new TransactionTemplate(transactionManager)
          .executeWithoutResult(
              status ->
                  jdbcTemplate.update(
                      "UPDATE STM_TREE SET TRE_NAME = ? WHERE TRE_ID = ?", original, TREE_ID));
    }
  }
}
