package org.sitmun.domain.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.sitmun.test.TestUtils.asJsonString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.i18n.LiteralTranslationEnsureService;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslation;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.LiteralTranslationValueRepository;
import org.sitmun.test.BaseTest;
import org.sitmun.test.URIConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@DisplayName("Task Data REST literal enrollment")
class TaskLiteralEnrollmentDataRestTest extends BaseTest {

  @Autowired private TaskRepository taskRepository;
  @Autowired private LiteralTranslationRepository literalTranslationRepository;
  @Autowired private LiteralTranslationValueRepository literalTranslationValueRepository;

  @MockitoSpyBean private LiteralTranslationEnsureService literalTranslationEnsureService;

  @Test
  @WithMockUser(roles = "ADMIN")
  @Transactional
  @DisplayName("F9: POST Task with nested <t> enrolls exact inner HTML key")
  void enrollsNestedTipTapKeyOnCreate() throws Exception {
    String nestedKey = "<strong>Hola-nested-" + System.nanoTime() + "</strong>";
    String taskName = "literal-enroll-" + System.nanoTime();

    Map<String, Object> properties = new HashMap<>();
    properties.put("templateHtml", "<p><t>" + nestedKey + "</t></p>");

    Task newTask = Task.builder().name(taskName).properties(properties).build();

    mvc.perform(
            post(URIConstants.TASKS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(newTask)))
        .andExpect(status().isCreated());

    LiteralTranslation enrolled =
        literalTranslationRepository.findByLiteral(nestedKey).orElseThrow();
    assertThat(enrolled.getSourceLanguage().getShortname()).isEqualTo("en");
    assertThat(
            literalTranslationValueRepository.findValueByLiteralIdAndLanguage(
                enrolled.getId(), "en"))
        .contains(nestedKey);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  @DisplayName("B3: ensure failure prevents Task create")
  void ensureFailureAbortsTaskCreate() throws Exception {
    String key = "abort-ensure-" + System.nanoTime();
    String taskName = "literal-abort-" + System.nanoTime();

    doThrow(new IllegalStateException("ensure failed for test"))
        .when(literalTranslationEnsureService)
        .ensureLiteralsFromTemplateHtml(contains(key));

    Map<String, Object> properties = new HashMap<>();
    properties.put("templateHtml", "<p><t>" + key + "</t></p>");
    Task newTask = Task.builder().name(taskName).properties(properties).build();

    mvc.perform(
            post(URIConstants.TASKS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(newTask)))
        .andExpect(status().is5xxServerError());

    assertThat(taskRepository.findAll()).noneMatch(task -> taskName.equals(task.getName()));
    assertThat(literalTranslationRepository.findByLiteral(key)).isEmpty();
  }
}
