package org.sitmun.domain.task.ui;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "task ui")
@RepositoryRestResource(collectionResourceRel = "task-uis", path = "task-uis")
public interface TaskUIRepository extends JpaRepository<TaskUI, Integer> {

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select taskUI
      from TaskUI taskUI
      where lower(taskUI.name) like lower(concat('%', :q, '%'))
      """)
  Page<TaskUI> findByContent(@Param("q") String q, Pageable pageable);
}
