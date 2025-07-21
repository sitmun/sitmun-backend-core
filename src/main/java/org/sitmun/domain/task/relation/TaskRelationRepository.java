package org.sitmun.domain.task.relation;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "task relation")
@RepositoryRestResource(collectionResourceRel = "task-relations", path = "task-relations")
public interface TaskRelationRepository extends JpaRepository<TaskRelation, Integer> {
}