package org.sitmun.domain.tree;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;
import org.sitmun.domain.role.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

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

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select tree
      from Tree tree
      where lower(tree.name) like lower(concat('%', :q, '%'))
      """)
  Page<Tree> findByContent(@Param("q") String q, Pageable pageable);

  @Query(value = "SELECT TRE_TYPE FROM STM_TREE WHERE TRE_ID = ?1", nativeQuery = true)
  Optional<String> findPersistedTypeById(Integer treeId);
}
