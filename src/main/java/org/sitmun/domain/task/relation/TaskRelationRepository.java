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

  /**
   * Loads template child edges with related-task to-ones that are JPA-EAGER ({@code type}, {@code
   * group}, {@code connection}, …). Fetching only {@code relatedTask}/{@code type} leaves other
   * EAGER associations to nested selects while the join ResultSet is still open, which PostgreSQL
   * closes ({@code ResultSet is closed} / column extract failures) during Plantilla Execute.
   */
  @RestResource(exported = false)
  @Query(
      """
      select tr from TaskRelation tr
      join fetch tr.relatedTask rt
      left join fetch rt.type
      left join fetch rt.group
      left join fetch rt.connection
      left join fetch rt.cartography
      left join fetch rt.service
      left join fetch rt.ui
      where tr.task.id = ?1
      """)
  List<TaskRelation> findByTaskId(Integer taskId);
}
