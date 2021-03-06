package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.Tree;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "tree")
@RepositoryRestResource(collectionResourceRel = "trees", path = "trees"/*, excerptProjection = TreeProjection.class*/)
public interface TreeRepository extends PagingAndSortingRepository<Tree, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Tree> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull Tree entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Tree', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<Tree> findAll();

  @Query("select tree from Tree tree left join fetch tree.allNodes where tree.id =:id")
  Tree findOneWithEagerRelationships(long id);

}