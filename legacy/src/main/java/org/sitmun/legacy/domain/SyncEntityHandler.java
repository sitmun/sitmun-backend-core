package org.sitmun.legacy.domain;

import org.sitmun.common.domain.task.Task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SyncEntityHandler {
  String PARAMETERS = "parameters";

  void synchronize();

  default void updateParameters(Task task, List<?> parameters) {
    Map<String, Object> properties = task.getProperties();
    if (parameters.isEmpty() && properties != null) {
      properties.remove(PARAMETERS);
      task.setProperties(properties);
    } else if (!parameters.isEmpty() && properties != null) {
      properties.put(PARAMETERS, parameters);
      task.setProperties(properties);
    } else if (!parameters.isEmpty()) {
      properties = new HashMap<>();
      properties.put(PARAMETERS, parameters);
      task.setProperties(properties);
    }
  }

  default void updateProperty(Task task, String property, Object value) {
    Map<String, Object> properties = task.getProperties();
    if (value == null && properties != null) {
      properties.remove(property);
      task.setProperties(properties);
    } else if (value != null && properties != null) {
      properties.put(property, value);
      task.setProperties(properties);
    } else if (value != null) {
      properties = new HashMap<>();
      properties.put(property, value);
      task.setProperties(properties);
    }
  }

}
