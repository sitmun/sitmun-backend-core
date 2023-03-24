package org.sitmun.domain.task.basic;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.parameter.ParameterUtils;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterRepository;
import org.sitmun.infrastructure.persistence.liquibase.SyncEntityHandler;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@RepositoryEventHandler
@Slf4j
public class BasicTaskEventHandler implements SyncEntityHandler {

  private static final Integer BASIC_TASK = 1;
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
    if (nonAccept(task)) return;
    taskParameterRepository.deleteAllByTask(task);

    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      Function<BasicParameter, TaskParameter> converter = parameter -> TaskParameter.builder()
        .task(task)
        .name(parameter.getName())
        .value(parameter.getValue())
        .type(parameter.getType())
        .order(parameter.getOrder()).build();
      List<TaskParameter> parameters = ParameterUtils.collect(properties,
        "$.parameters[?(@.name && @.value && @.type && @.order)]",
        BasicParameter.class,
        converter);
      taskParameterRepository.saveAll(parameters);
    }
  }

  @HandleBeforeDelete
  @Transactional
  public void deleteParameters(@NonNull Task task) {
    if (nonAccept(task)) return;
    taskParameterRepository.deleteAllByTask(task);
  }

  public boolean nonAccept(@NonNull Task task) {
    return task.getType() == null || !task.getType().getId().equals(BASIC_TASK);
  }

  public void synchronize() {
    log.info("Rebuilding properties for Basic tasks (task type = " + BASIC_TASK + ")");
    taskRepository.findAllByTypeId(BASIC_TASK).forEach(task -> {
        List<BasicParameter> parameters = StreamSupport.stream(taskParameterRepository.findAllByTask(task).spliterator(), false)
          .map(parameter -> BasicParameter.builder()
            .name(parameter.getName())
            .value(parameter.getValue())
            .type(parameter.getType())
            .order(parameter.getOrder()).build())
          .collect(Collectors.toList());
        updateParameters(task, parameters);
        taskRepository.save(task);
      }
    );
  }

}

