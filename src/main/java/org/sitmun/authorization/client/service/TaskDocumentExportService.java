package org.sitmun.authorization.client.service;

import static org.sitmun.authorization.client.AuthorizationConstants.TaskDto.SIMPLE;
import static org.sitmun.domain.DomainConstants.Tasks.PROFILE_LAYER_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_DOWNLOAD_SOURCE;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_EXPORT_ENGINE;
import static org.sitmun.domain.DomainConstants.Tasks.SCOPE_RESOURCE;
import static org.sitmun.domain.DomainConstants.Tasks.TASK_PROFILE_ID_PREFIX;
import static org.sitmun.domain.DomainConstants.Tasks.isDocumentExportTask;

import java.util.LinkedHashMap;
import java.util.Map;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.territory.Territory;
import org.springframework.stereotype.Component;

/** Publishes document export tasks in the client profile so the viewer can discover them globally. */
@Component
public class TaskDocumentExportService implements TaskMapper {

  @Override
  public boolean accept(Task task) {
    return isDocumentExportTask(task);
  }

  @Override
  public TaskDto map(Task task, Application application, Territory territory) {
    Map<String, Object> parameters = new LinkedHashMap<>();
    Map<String, Object> properties = task.getProperties();

    if (properties != null) {
      copyStringProperty(parameters, properties, PROPERTY_EXPORT_ENGINE);
      copyStringProperty(parameters, properties, PROPERTY_DOWNLOAD_FORMAT);
      copyStringProperty(parameters, properties, PROPERTY_DOWNLOAD_SOURCE);
      copyStringProperty(parameters, properties, "output", PROPERTY_DOWNLOAD_FORMAT);
    }

    String cartographyProfileId =
        task.getCartography() != null
            ? PROFILE_LAYER_ID_PREFIX + task.getCartography().getId()
            : null;

    return TaskDto.builder()
        .id(TASK_PROFILE_ID_PREFIX + task.getId())
        .name(task.getName())
        .type(SIMPLE)
        .typeId(task.getType() != null ? task.getType().getId() : null)
        .scope(SCOPE_RESOURCE)
        .parameters(parameters.isEmpty() ? null : parameters)
        .layer(cartographyProfileId)
        .build();
  }

  private void copyStringProperty(
      Map<String, Object> target, Map<String, Object> source, String propertyName) {
    copyStringProperty(target, source, propertyName, propertyName);
  }

  private void copyStringProperty(
      Map<String, Object> target,
      Map<String, Object> source,
      String targetKey,
      String sourceKey) {
    Object value = source.get(sourceKey);
    if (value != null) {
      target.put(targetKey, String.valueOf(value));
    }
  }
}