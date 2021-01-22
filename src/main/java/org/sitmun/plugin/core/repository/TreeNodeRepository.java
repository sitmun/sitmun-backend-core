package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.TreeNode;
import org.sitmun.plugin.core.domain.projections.TreeNodeProjection;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "tree node")
@RepositoryRestResource(collectionResourceRel = "tree-nodes", path = "tree-nodes",
  excerptProjection = TreeNodeProjection.class)
public interface TreeNodeRepository extends CrudRepository<TreeNode, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends TreeNode> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull TreeNode entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.TreeNode', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<TreeNode> findAll();

}