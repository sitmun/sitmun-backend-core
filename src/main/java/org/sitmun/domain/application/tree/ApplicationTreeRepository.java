package org.sitmun.domain.application.tree;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.tree.OrderedTree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "application tree")
@RepositoryRestResource(collectionResourceRel = "application-trees", path = "application-trees")
public interface ApplicationTreeRepository extends JpaRepository<ApplicationTree, Integer> {

  @RestResource(exported = false)
  @Query(
      """
      select new org.sitmun.domain.tree.OrderedTree(at.order, tree)
      from ApplicationTree at
      join at.tree tree
      join tree.availableRoles role
      where at.application.id = ?1 and role in ?2
      order by at.order asc, tree.id asc
      """)
  List<OrderedTree> findByAppAndRoles(Integer appId, List<Role> roles);
}
