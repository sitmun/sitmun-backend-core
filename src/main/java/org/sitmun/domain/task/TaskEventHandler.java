package org.sitmun.domain.task;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.administration.service.i18n.LiteralTranslationEnsureService;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RepositoryEventHandler
@Slf4j
public class TaskEventHandler {

  private final List<TaskValidator> taskValidator;
  private final LiteralTranslationEnsureService literalTranslationEnsureService;

  TaskEventHandler(
      List<TaskValidator> taskValidator,
      LiteralTranslationEnsureService literalTranslationEnsureService) {
    this.taskValidator = taskValidator;
    this.literalTranslationEnsureService = literalTranslationEnsureService;
  }

  @HandleBeforeSave
  @HandleBeforeCreate
  public void handleTaskCreate(@NotNull Task task) {

    for (TaskValidator validator : taskValidator) {
      if (validator.accept(task)) {
        validator.validate(task);
      }
    }

    ensureTemplateLiterals(task);
  }

  private void ensureTemplateLiterals(Task task) {
    Map<String, Object> properties = task.getProperties();
    if (properties == null) {
      return;
    }
    Object templateHtml = properties.get("templateHtml");
    if (!(templateHtml instanceof String html) || !StringUtils.hasText(html)) {
      return;
    }
    literalTranslationEnsureService.ensureLiteralsFromTemplateHtml(html);
  }
}
