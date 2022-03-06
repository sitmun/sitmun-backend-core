package org.sitmun.repository.handlers.stm2;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.common.domain.task.Task;
import org.sitmun.common.domain.task.TaskRepository;
import org.sitmun.domain.TaskParameter;
import org.sitmun.repository.TaskParameterRepository;
import org.sitmun.repository.handlers.SyncEntityHandler;
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
    if (!accept(task)) return;
    taskParameterRepository.deleteAllByTask(task);
  }

  public Boolean accept(@NonNull Task task) {
    return task.getType() != null && task.getType().getId().equals(BASIC_TASK);
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

