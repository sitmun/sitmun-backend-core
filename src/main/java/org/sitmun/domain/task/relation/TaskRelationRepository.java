package org.sitmun.domain.task.relation;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "task relation")
@RepositoryRestResource(collectionResourceRel = "task-relations", path = "task-relations")
public interface TaskRelationRepository extends JpaRepository<TaskRelation, Integer> {

  @RestResource(exported = false)
  @Query("select tr from TaskRelation tr join fetch tr.relatedTask rt left join fetch rt.type where tr.task.id = ?1")
  List<TaskRelation> findByTaskId(Integer taskId);
}
