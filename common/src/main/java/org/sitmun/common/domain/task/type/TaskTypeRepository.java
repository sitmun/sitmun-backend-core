package org.sitmun.common.domain.task.type;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "task type")
@RepositoryRestResource(collectionResourceRel = "task-types", path = "task-types")
public interface TaskTypeRepository extends PagingAndSortingRepository<TaskType, Integer> {
}