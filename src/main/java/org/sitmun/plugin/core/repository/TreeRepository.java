package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import org.sitmun.plugin.core.domain.Tree;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "tree")
@RepositoryRestResource(collectionResourceRel = "trees", path = "trees"/*, excerptProjection = TreeProjection.class*/)
public interface TreeRepository extends CrudRepository<Tree, BigInteger> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends Tree> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Tree entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Tree', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(#entity, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Tree> findAll();

  @Query("select tree from Tree tree left join fetch tree.allNodes where tree.id =:id")
  Tree findOneWithEagerRelationships(long id);

}