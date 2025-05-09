package org.sitmun.domain.task;

import org.sitmun.domain.task.type.TaskType;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import javax.validation.constraints.NotNull;
import java.util.*;

public interface TaskValidator {

  boolean accept(Task task);

  void validate(Task task) throws RepositoryConstraintViolationException;

  default Errors init(@NotNull Task task) {
    TaskType type = task.getType();
    String typeName = type != null ? type.getTitle() : "unknown";
    return new BeanPropertyBindingResult(task, "Task "+typeName);
  }

  default @NotNull Map<String, Object> getProperties(@NotNull Task task) {
    Map<String, Object> map = task.getProperties();
    if (map == null) {
      return Collections.emptyMap();
    } else {
      return map;
    }
  }

  default @NotNull List<Map<String, Object>> getParameters(@NotNull Task task) {
    Map<String, Object> map = task.getProperties();
    if (map == null) {
      return Collections.emptyList();
    }

    if (!map.containsKey("parameters")) {
      Errors errors = init(task);
      errors.rejectValue("properties", "parameters.missing", "[parameters] property was expected");
      throw new RepositoryConstraintViolationException(errors);
    }

    Object rawParameters= map.get("parameters");
    if(!(rawParameters instanceof List)) {
      Errors errors = init(task);
      errors.rejectValue("properties", "parameters.notList", "[parameters] property must be a list");
      throw new RepositoryConstraintViolationException(errors);
    }

    @SuppressWarnings("unchecked")
    List<Object> parameters = (List<Object>) rawParameters;
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object obj : parameters) {
      if (!(obj instanceof Map)) {
        Errors errors = init(task);
        errors.rejectValue("properties", "parameters.notListOfMaps", "[parameters] must be a list of maps");
        throw new RepositoryConstraintViolationException(errors);
      }
      //noinspection unchecked
      result.add((Map<String, Object>) obj);
    }
    return result;
  }
}
