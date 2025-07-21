package org.sitmun.domain.task.availability;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "task availability")
@RepositoryRestResource(collectionResourceRel = "task-availabilities", path = "task-availabilities")
public interface TaskAvailabilityRepository extends
  org.springframework.data.jpa.repository.JpaRepository<TaskAvailability, Integer> {
}