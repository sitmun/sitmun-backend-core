package org.sitmun.domain.task.query;

import com.google.common.collect.ImmutableMap;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.*;

@Component
@RepositoryEventHandler
@Slf4j
public class QueryTaskEventHandler implements SyncEntityHandler {

  private static final String LABEL = "LABEL";
  private static final String TIPO = "TIPO";
  private static final String SQL = "SQL";
  private static final Integer LOCATOR_TASK = 4;
  private static final Integer QUERY_TASK = 5;
  private final TaskRepository taskRepository;

  private final QueryTaskRepository queryTaskRepository;

  private final TaskParameterRepository taskParameterRepository;

  public QueryTaskEventHandler(TaskRepository taskRepository, QueryTaskRepository queryTaskRepository, TaskParameterRepository taskParameterRepository) {
    this.taskRepository = taskRepository;
    this.queryTaskRepository = queryTaskRepository;
    this.taskParameterRepository = taskParameterRepository;
  }

  @HandleAfterCreate
  @HandleAfterSave
  @Transactional
  public void updateDownloadTasks(@NonNull Task task) {
    if (nonAccept(task)) {
        return;
    }
    taskParameterRepository.deleteAllByTask(task);
    if (queryTaskRepository.existsById(task.getId())) {
      queryTaskRepository.deleteById(task.getId());
    }

    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      QueryTask queryTask = ParameterUtils.obtain(properties, "$", QueryTask.class);
      queryTask.setId(task.getId());
      queryTaskRepository.save(queryTask);


      Function<QueryParameter, Stream<TaskParameter>> convert = parameter -> computeTaskParameters(task, parameter);
      List<TaskParameter> parameters = ParameterUtils.flatCollect(properties,
        "$." + PARAMETERS + "[?(@.key && @.type && @.label && @.select && @.order)]",
        QueryParameter.class,
        convert
      );
      taskParameterRepository.saveAll(parameters);
    }

  }

  private static Stream<TaskParameter> computeTaskParameters(Task task, QueryParameter parameter) {
    return Stream.of(
      TaskParameter.builder()
        .task(task)
        .name(parameter.getKey())
        .value(parameter.getLabel())
        .type(LABEL)
        .order(parameter.getOrder()).build()
      ,
      TaskParameter.builder()
        .task(task)
        .name(parameter.getKey())
        .value(parameter.getType())
        .type(TIPO)
        .order(parameter.getOrder()).build()
      ,
      TaskParameter.builder()
        .task(task)
        .name(parameter.getKey())
        .value(parameter.getValue())
        .type(SQL)
        .order(parameter.getOrder()).build()
    );
  }

  @HandleBeforeDelete
  @Transactional
  public void deleteParameters(@NonNull Task task) {
    if (nonAccept(task)) {
        return;
    }
    taskParameterRepository.deleteAllByTask(task);
    if (queryTaskRepository.existsById(task.getId())) {
      queryTaskRepository.deleteById(task.getId());
    }
  }

  public boolean nonAccept(@NonNull Task task) {
    return task.getType() == null || ((!task.getType().getId().equals(LOCATOR_TASK)) &&
      (!task.getType().getId().equals(QUERY_TASK)));
  }

  public void synchronize() {
    log.info("Rebuilding properties for Locator tasks (task type = " + LOCATOR_TASK + ")");
    taskRepository.saveAll(
      StreamSupport.stream(taskRepository.findAllByTypeId(LOCATOR_TASK).spliterator(), false)
        .map(this::updateTask)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::updateProperties)
        .collect(toList()));

    log.info("Rebuilding properties for Query tasks (task type = " + QUERY_TASK + ")");
    taskRepository.saveAll(
      StreamSupport.stream(taskRepository.findAllByTypeId(QUERY_TASK).spliterator(), false)
        .map(this::updateTask)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(this::updateProperties)
        .collect(toList()));
  }

  private Task updateProperties(Task task) {
    List<QueryParameter> parameters = extractParameters(task);
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
    }
      return Optional.empty();
  }

  List<QueryParameter> extractParameters(Task task) {
    return StreamSupport
      .stream(taskParameterRepository.findAllByTask(task).spliterator(), false)
      .collect(groupingBy(TaskParameter::getName, toList()))
      .values()
      .stream()
      .map(taskParameters -> {
        final QueryParameter qp = new QueryParameter();
        taskParameters.forEach(row -> {
          qp.setKey(row.getName());
          if (TIPO.equals(row.getType())) {
            qp.setType(row.getValue());
          } else if (LABEL.equals(row.getType())) {
            qp.setLabel(row.getValue());
          } else if (SQL.equals(row.getType())) {
            qp.setValue(row.getValue());
          }
          qp.setOrder(row.getOrder());
        });
        Optional<QueryParameter> result;
        if (qp.getType() != null && qp.getLabel() != null && qp.getValue() != null) {
          result = Optional.of(qp);
        } else {
          String fail = ImmutableMap.of(
            TIPO, Optional.ofNullable(qp.getType()),
            LABEL, Optional.ofNullable(qp.getLabel()),
            SQL, Optional.ofNullable(qp.getValue())).entrySet().stream().map(
            it -> {
              Optional<String> r;
              if (it.getValue().isPresent()) {
                  r = Optional.empty();
              } else {
                  r = Optional.of(it.getKey());
              }
              return r;
            }
          ).filter(Optional::isPresent).map(Optional::get).collect(joining(", "));
          log.info("For Task {} (task type = {}) parameter {} not extracted: {} param rows not found", task.getId(), task.getType().getId(), qp.getKey(), fail);
          result = Optional.empty();
        }
        return result;
      }).filter(Optional::isPresent)
      .map(Optional::get)
      .collect(toList());
  }

}

