package org.sitmun.domain.tree;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.role.Role;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@Tag(name = "tree")
@RepositoryRestResource(collectionResourceRel = "trees", path = "trees"/*, excerptProjection = TreeProjection.class*/)
public interface TreeRepository extends PagingAndSortingRepository<Tree, Integer> {
  @Query("select tree from Tree tree left join fetch tree.allNodes where tree.id = ?1")
  Tree findOneWithEagerRelationships(Integer id);

  @Query("select distinct tree from Tree tree, Application app, Role role where " +
    "app member tree.availableApplications and app.id = ?1 AND " +
    "role member tree.availableRoles and role in ?2")
  List<Tree> findByAppAndRoles(Integer appId, List<Role> roles);
}