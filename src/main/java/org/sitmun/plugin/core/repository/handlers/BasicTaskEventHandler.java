package org.sitmun.plugin.core.repository.handlers;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.TypeRef;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import lombok.*;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.sitmun.plugin.core.repository.TaskParameterRepository;
import org.sitmun.plugin.core.repository.TaskRepository;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@RepositoryEventHandler
public class BasicTaskEventHandler {

  public final static String PARAMETERS = "parameters";
  private final static Integer BASIC_TASK = 1;
  private final TaskRepository taskRepository;

  private final TaskParameterRepository taskParameterRepository;

  public BasicTaskEventHandler(TaskRepository taskRepository, TaskParameterRepository taskParameterRepository) {
    this.taskRepository = taskRepository;
    this.taskParameterRepository = taskParameterRepository;
  }

  @HandleAfterCreate
  @HandleAfterSave
  @Transactional
  public void updateParameters(@NonNull Task task) {
    if (!accept(task)) return;
    taskParameterRepository.deleteAllByTask(task);
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      TypeRef<List<BasicParameter>> typeRef = new TypeRef<List<BasicParameter>>() {
      };
      Configuration conf = Configuration.defaultConfiguration()
        .mappingProvider(new JacksonMappingProvider())
        .addOptions(Option.ALWAYS_RETURN_LIST);
      List<TaskParameter> parameters =
        JsonPath.using(conf).parse(properties)
          .read("$." + PARAMETERS + "[?(@.name && @.value && @.type && @.order)]", typeRef)
          .stream()
          .map(parameter -> TaskParameter.builder()
            .task(task)
            .name(parameter.getName())
            .value(parameter.getValue())
            .type(parameter.getType())
            .order(parameter.getOrder()).build()
          ).collect(Collectors.toList());
      taskParameterRepository.saveAll(parameters);
    }
  }

  @HandleBeforeDelete
  @Transactional
  public void deleteParameters(@NonNull Task task) {
    if (!accept(task)) return;
    taskParameterRepository.deleteAllByTask(task);
  }

  public Boolean accept(@NonNull Task task) {
    return task.getType() != null && task.getType().getId().equals(BASIC_TASK);
  }

  public void updateTaskProperties() {
    taskRepository.findAllByTypeId(BASIC_TASK).forEach(task ->
      taskParameterRepository.saveAll(StreamSupport.stream(
        taskParameterRepository.findAllByTask(task).spliterator(), false)
        .map(parameter ->
          TaskParameter.builder()
            .task(task)
            .type(parameter.getType())
            .name(parameter.getName())
            .value(parameter.getValue())
            .order(parameter.getOrder())
            .build()
        ).collect(Collectors.toList()))
    );
  }

  public void syncTaskProperties() {
    taskRepository.findAllByTypeId(BASIC_TASK).forEach(task -> {
        List<BasicParameter> parameters = StreamSupport.stream(taskParameterRepository.findAllByTask(task).spliterator(), false)
          .map(parameter -> BasicParameter.builder()
            .name(parameter.getName())
            .value(parameter.getValue())
            .type(parameter.getType())
            .order(parameter.getOrder()).build())
          .collect(Collectors.toList());
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
        taskRepository.save(task);
      }
    );
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @Getter
  @Setter
  static class BasicParameter {
    private String name;
    private String value;
    private String type;
    private Integer order;
  }
}

