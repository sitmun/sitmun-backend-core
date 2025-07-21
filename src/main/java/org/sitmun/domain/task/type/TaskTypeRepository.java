package org.sitmun.domain.task.type;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "task type")
@RepositoryRestResource(collectionResourceRel = "task-types", path = "task-types")
public interface TaskTypeRepository extends JpaRepository<TaskType, Integer> {
}