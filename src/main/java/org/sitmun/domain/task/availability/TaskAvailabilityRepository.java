package org.sitmun.domain.task.availability;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "task availability")
@RepositoryRestResource(collectionResourceRel = "task-availabilities", path = "task-availabilities")
public interface TaskAvailabilityRepository
    extends org.springframework.data.jpa.repository.JpaRepository<TaskAvailability, Integer> {

  @RestResource(exported = false)
  @Query("select ta from TaskAvailability ta join fetch ta.territory where ta.task.id = ?1")
  List<TaskAvailability> findByTaskId(Integer taskId);
}
