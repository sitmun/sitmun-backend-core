package org.sitmun.domain.tree;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.role.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "tree")
@RepositoryRestResource(
    collectionResourceRel = "trees",
    path = "trees" /*, excerptProjection = TreeProjection.class*/)
public interface TreeRepository extends JpaRepository<Tree, Integer> {
  @Query("select tree from Tree tree left join fetch tree.allNodes where tree.id = ?1")
  Tree findOneWithEagerRelationships(Integer id);

  @EntityGraph(attributePaths = {"availableRoles", "availableApplications"})
  @Query(
      """
      SELECT tree
      FROM Tree tree
      WHERE tree.id IN (
        SELECT DISTINCT tree2.id
        FROM Tree tree2
        JOIN tree2.availableApplications app
        JOIN tree2.availableRoles role
        WHERE app.id = ?1 AND role in ?2
      )
      """)
  List<Tree> findByAppAndRoles(Integer appId, List<Role> roles);
}
