package org.sitmun.plugin.core.repository;


import java.math.BigInteger;
import org.sitmun.plugin.core.domain.Tree;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "trees", path = "trees"/*, excerptProjection = TreeProjection.class*/)
public interface TreeRepository extends CrudRepository<Tree, BigInteger> {
  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  Tree save(@P("entity") Tree entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Tree entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Tree', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(#entity, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Tree> findAll();

  @Query("select tree from Tree tree left join fetch tree.nodes where tree.id =:id")
  Tree findOneWithEagerRelationships(long id);


}