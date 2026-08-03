package org.sitmun.infrastructure.persistence.type.i18n;

import static org.sitmun.test.URIConstants.TASKS_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Oracle-only regression for ORA-17010 (Closed ResultSet) during Task collection hydration.
 *
 * <p>Production repro (admin tree form): {@code GET /api/tasks?type.id=5&lang=en&size=200&page=0
 * &projection=view}. While Hibernate reads the Task ResultSet, associated {@code TaskGroup}
 * {@code @PostLoad} calls {@link DatabaseDefaultLanguageResolver#resolveShortname()}, which queries
 * {@code STM_CONF} and closes the parent Oracle ResultSet.
 *
 * <p>Run: {@code ./gradlew testOracle --tests
 * org.sitmun.infrastructure.persistence.type.i18n.OraclePostLoadNestedQueryIT}
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*oracle.*")
@DisplayName("Oracle PostLoad nested query (ORA-17010)")
class OraclePostLoadNestedQueryIT {

  @Autowired private MockMvc mvc;

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("GET /api/tasks type.id=5 with lang+projection returns 200 (no ORA-17010)")
  void queryTasksWithLangAndProjectionDoNotCloseResultSet() throws Exception {
    mvc.perform(
            get(TASKS_URI)
                .param("type.id", "5")
                .param("lang", "en")
                .param("size", "200")
                .param("page", "0")
                .param("projection", "view"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.tasks").isArray());
  }
}
