package org.sitmun.domain.task.ui;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "task ui")
@RepositoryRestResource(collectionResourceRel = "task-uis", path = "task-uis")
public interface TaskUIRepository extends JpaRepository<TaskUI, Integer> {
}