package org.sitmun.plugin.core.repository.handlers;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.plugin.core.domain.QueryTask;
import org.sitmun.plugin.core.domain.Task;
import org.sitmun.plugin.core.domain.TaskParameter;
import org.sitmun.plugin.core.repository.QueryTaskRepository;
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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@Component
@RepositoryEventHandler
@Slf4j
public class AdditionalInfoTaskEventHandler implements SyncEntityHandler {

  private final static Integer ADDITIONAL_INFO_TASK = 6;
  private final TaskRepository taskRepository;

  private final QueryTaskRepository queryTaskRepository;

  private final TaskParameterRepository taskParameterRepository;

  public AdditionalInfoTaskEventHandler(TaskRepository taskRepository, QueryTaskRepository queryTaskRepository, TaskParameterRepository taskParameterRepository) {
    this.taskRepository = taskRepository;
    this.queryTaskRepository = queryTaskRepository;
    this.taskParameterRepository = taskParameterRepository;
  }

  @HandleAfterCreate
  @HandleAfterSave
  @Transactional
  public void updateDownloadTasks(@NonNull Task task) {
    if (!accept(task)) return;
    if (queryTaskRepository.existsById(task.getId())) {
      queryTaskRepository.deleteById(task.getId());
    }
    taskParameterRepository.deleteAllByTask(task);

    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      QueryTask queryTask = ParameterUtils.obtain(properties, "$", QueryTask.class);
      queryTask.setId(task.getId());
      queryTaskRepository.save(queryTask);

      Function<BasicParameter, TaskParameter> convert = parameter -> TaskParameter.builder()
        .task(task)
        .name(parameter.getName())
        .value(parameter.getValue())
        .order(parameter.getOrder())
        .type(parameter.getType())
        .build();
      List<TaskParameter> parameters = ParameterUtils.collect(properties,
        "$." + PARAMETERS + "[?(@.name && @.order && @.value && @.type)]",
        BasicParameter.class,
        convert
      );
      taskParameterRepository.saveAll(parameters);
    }

  }

  @HandleBeforeDelete
  @Transactional
  public void deleteParameters(@NonNull Task task) {
    if (!accept(task)) return;
    if (queryTaskRepository.existsById(task.getId())) {
      queryTaskRepository.deleteById(task.getId());
    }
    taskParameterRepository.deleteAllByTask(task);
  }

  public Boolean accept(@NonNull Task task) {
    return task.getType() != null && task.getType().getId().equals(ADDITIONAL_INFO_TASK);
  }

  public void synchronize() {
    log.info("Rebuilding properties for Additional Info tasks (task type = " + ADDITIONAL_INFO_TASK + ")");
    taskRepository.saveAll(
      StreamSupport.stream(taskRepository.findAllByTypeId(ADDITIONAL_INFO_TASK).spliterator(), false)
        .map(this::updateTask)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::updateProperties)
        .collect(toList()));

  }

  private Task updateProperties(Task task) {
    List<BasicParameter> parameters = extractParameters(task);
    updateParameters(task, parameters);
    return task;
  }

  private Optional<Task> updateTask(Task task) {
    Optional<QueryTask> queryTask = queryTaskRepository.findById(task.getId());
    if (queryTask.isPresent()) {
      Map<String, Object> newProperties = new HashMap<>();
      newProperties.put("command", queryTask.get().getCommand());
      newProperties.put("scope", queryTask.get().getScope());
      newProperties.put("description", queryTask.get().getDescription());
      task.setProperties(newProperties);
      return Optional.of(task);
    } else {
      return Optional.empty();
    }
  }

  List<BasicParameter> extractParameters(Task task) {
    return StreamSupport
      .stream(taskParameterRepository.findAllByTask(task).spliterator(), false)
      .map(parameter -> BasicParameter.builder()
        .name(parameter.getName())
        .value(parameter.getValue())
        .type(parameter.getType())
        .order(parameter.getOrder()).build())
      .collect(Collectors.toList());
  }

}

