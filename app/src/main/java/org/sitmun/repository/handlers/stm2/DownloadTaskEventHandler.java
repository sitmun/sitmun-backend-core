package org.sitmun.repository.handlers.stm2;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.DownloadTask;
import org.sitmun.domain.Task;
import org.sitmun.repository.DownloadTaskRepository;
import org.sitmun.repository.TaskRepository;
import org.sitmun.repository.handlers.SyncEntityHandler;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component
@RepositoryEventHandler
@Slf4j
public class DownloadTaskEventHandler implements SyncEntityHandler {

  private final static Integer DOWNLOAD_TASK = 2;
  private final static Integer ATTACHMENT_TASK = 3;
  private final TaskRepository taskRepository;

  private final DownloadTaskRepository downloadTaskRepository;

  public DownloadTaskEventHandler(TaskRepository taskRepository, DownloadTaskRepository downloadTaskRepository) {
    this.taskRepository = taskRepository;
    this.downloadTaskRepository = downloadTaskRepository;
  }

  @HandleAfterCreate
  @HandleAfterSave
  @Transactional
  public void updateDownloadTasks(@NonNull Task task) {
    if (!accept(task)) return;
    if (downloadTaskRepository.existsById(task.getId())) {
      downloadTaskRepository.deleteById(task.getId());
    }
    Map<String, Object> properties = task.getProperties();
    if (properties != null) {
      DownloadTask downloadTask = ParameterUtils.obtain(properties, "$", DownloadTask.class);
      downloadTask.setId(task.getId());
      if (task.getType().getId().equals(DOWNLOAD_TASK)) {
        downloadTask.setScope("U");
      }
      downloadTaskRepository.save(downloadTask);
    }
  }

  @HandleBeforeDelete
  @Transactional
  public void deleteParameters(@NonNull Task task) {
    if (!accept(task)) return;
    if (downloadTaskRepository.existsById(task.getId())) {
      downloadTaskRepository.deleteById(task.getId());
    }
  }

  public Boolean accept(@NonNull Task task) {
    return task.getType() != null &&
      (task.getType().getId().equals(DOWNLOAD_TASK) ||
        task.getType().getId().equals(ATTACHMENT_TASK));
  }

  public void synchronize() {
    log.info("Rebuilding properties for Download tasks (task type = " + DOWNLOAD_TASK + ")");
    taskRepository.saveAll(
      StreamSupport.stream(taskRepository.findAllByTypeId(DOWNLOAD_TASK).spliterator(), false)
        .map(this::updateTask)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList()));

    log.info("Rebuilding properties for Attachment tasks (task type = " + ATTACHMENT_TASK + ")");
    taskRepository.saveAll(
      StreamSupport.stream(taskRepository.findAllByTypeId(ATTACHMENT_TASK).spliterator(), false)
        .map(this::updateTask)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList()));
  }

  private Optional<Task> updateTask(Task task) {
    Optional<DownloadTask> downloadTask = downloadTaskRepository.findById(task.getId());
    if (downloadTask.isPresent()) {
      Map<String, Object> newProperties = new HashMap<>();
      newProperties.put("format", downloadTask.get().getFormat());
      newProperties.put("scope", downloadTask.get().getScope());
      newProperties.put("path", downloadTask.get().getPath());
      task.setProperties(newProperties);
      return Optional.of(task);
    } else {
      return Optional.empty();
    }
  }
}

