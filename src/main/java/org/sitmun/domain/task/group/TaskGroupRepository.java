package org.sitmun.domain.task.group;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "task group")
@RepositoryRestResource(collectionResourceRel = "task-groups", path = "task-groups")
public interface TaskGroupRepository extends JpaRepository<TaskGroup, Integer> {

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select taskGroup
      from TaskGroup taskGroup
      where lower(taskGroup.name) like lower(concat('%', :q, '%'))
      """)
  Page<TaskGroup> findByContent(@Param("q") String q, Pageable pageable);
}
