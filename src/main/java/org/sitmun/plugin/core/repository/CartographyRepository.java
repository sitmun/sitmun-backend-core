package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.Cartography;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "cartography")
@RepositoryRestResource(collectionResourceRel = "cartographies", path = "cartographies"/*, excerptProjection = CartographyProjection.class*/)
public interface CartographyRepository extends CrudRepository<Cartography, BigInteger> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends Cartography> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Cartography entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Cartography> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography', 'read')")
  Optional<Cartography> findById(@P("entityId") BigInteger entityId);

  @Query("select cartography from Cartography cartography left join fetch cartography.service where cartography.id =:id")
  Cartography findOneWithEagerRelationships(BigInteger id);

}