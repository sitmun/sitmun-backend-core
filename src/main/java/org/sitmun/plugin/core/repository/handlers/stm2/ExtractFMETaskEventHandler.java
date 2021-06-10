package org.sitmun.plugin.core.repository.handlers.stm2;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.sitmun.plugin.core.repository.TaskParameterRepository;
import org.sitmun.plugin.core.repository.TaskRepository;
import org.sitmun.plugin.core.repository.handlers.SyncEntityHandler;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@RepositoryEventHandler
@Slf4j
public class ExtractFMETaskEventHandler implements SyncEntityHandler {

  private final static Integer EXTRACT_FME_TASK = 10;
  private final TaskRepository taskRepository;

  private final TaskParameterRepository taskParameterRepository;

  public ExtractFMETaskEventHandler(TaskRepository taskRepository, TaskParameterRepository taskParameterRepository) {
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
      Object layers = properties.getOrDefault("layers", null);
      if (layers instanceof List &&
        ((List<?>) layers).stream().allMatch(it -> it instanceof String)
      ) {
        List<String> list = ((List<?>) layers).stream().map(it -> (String) it).collect(Collectors.toList());
        TaskParameter parameter = TaskParameter.builder().task(task)
          .name("CAPAS")
          .type("FME")
          .order(1)
          .value(String.join(",", list)).build();
        taskParameterRepository.save(parameter);
      }
    }
  }

  @HandleBeforeDelete
  @Transactional
  public void deleteParameters(@NonNull Task task) {
    if (!accept(task)) return;
    taskParameterRepository.deleteAllByTask(task);
  }

  public Boolean accept(@NonNull Task task) {
    return task.getType() != null && task.getType().getId().equals(EXTRACT_FME_TASK);
  }

  public void synchronize() {
    log.info("Rebuilding properties for Extract FME tasks (task type = " + EXTRACT_FME_TASK + ")");
    taskRepository.findAllByTypeId(EXTRACT_FME_TASK).forEach(task ->
      StreamSupport.stream(taskParameterRepository.findAllByTask(task).spliterator(), false)
        .filter(parameter ->
          Objects.equals(parameter.getName(), "CAPAS") &&
            Objects.equals(parameter.getType(), "FME") &&
            Objects.equals(parameter.getOrder(), 1)
        ).findFirst()
        .ifPresent(parameter -> {
          updateProperty(task, "layers", parameter.getValue().split(","));
          taskRepository.save(task);
        })
    );
  }

}

