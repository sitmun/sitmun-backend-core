package org.sitmun.domain.tree;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.role.Role;
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

  @Query(
      """
      select distinct tree from Tree tree, Application app, Role role where
      app member of tree.availableApplications and app.id = ?1 AND
      role member of tree.availableRoles and role in ?2""")
  List<Tree> findByAppAndRoles(Integer appId, List<Role> roles);
}
