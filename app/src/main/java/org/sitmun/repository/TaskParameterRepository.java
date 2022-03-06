package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.common.domain.task.Task;
import org.sitmun.domain.TaskParameter;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.NonNull;

@Tag(name = "task parameter")
public interface TaskParameterRepository extends
  PagingAndSortingRepository<TaskParameter, Integer> {
  void deleteAllByTask(@NonNull Task task);

  Iterable<TaskParameter> findAllByTask(@NonNull Task task);
}