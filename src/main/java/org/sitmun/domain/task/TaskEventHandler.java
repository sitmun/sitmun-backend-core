package org.sitmun.domain.task;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.DomainConstants;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
@Slf4j
public class TaskEventHandler {

  private static final String DEPRECATED_PDF_HEADER_HEIGHT = "pdfHeaderHeightMm";
  private static final String DEPRECATED_PDF_FOOTER_HEIGHT = "pdfFooterHeightMm";

  private final List<TaskValidator> taskValidator;

  TaskEventHandler(List<TaskValidator> taskValidator) {
    this.taskValidator = taskValidator;
  }

  @HandleBeforeSave
  @HandleBeforeCreate
  public void handleTaskCreate(@NotNull Task task) {
    Map<String, Object> properties = task.getProperties();
    if (DomainConstants.Tasks.isTemplateTask(task)
        && properties != null
        && (properties.containsKey(DEPRECATED_PDF_HEADER_HEIGHT)
            || properties.containsKey(DEPRECATED_PDF_FOOTER_HEIGHT))) {
      Map<String, Object> sanitizedProperties = new HashMap<>(properties);
      sanitizedProperties.remove(DEPRECATED_PDF_HEADER_HEIGHT);
      sanitizedProperties.remove(DEPRECATED_PDF_FOOTER_HEIGHT);
      task.setProperties(sanitizedProperties);
    }

    for (TaskValidator validator : taskValidator) {
      if (validator.accept(task)) {
        validator.validate(task);
      }
    }
  }
}
