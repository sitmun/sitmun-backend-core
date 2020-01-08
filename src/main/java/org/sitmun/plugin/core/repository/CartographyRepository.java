package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import org.sitmun.plugin.core.domain.Cartography;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "cartographies", path = "cartographies"/*, excerptProjection = CartographyProjection.class*/)
public interface CartographyRepository extends CrudRepository<Cartography, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  Cartography save(@P("entity") Cartography entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Cartography entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Cartography> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography', 'read')")
  Cartography findOne(@P("entityId") BigInteger entityId);

  @Query("select cartography from Cartography cartography left join fetch cartography.service where cartography.id =:id")
  Cartography findOneWithEagerRelationships(BigInteger id);

}