package org.sitmun.plugin.core.repository;


import java.math.BigInteger;
import org.sitmun.plugin.core.domain.CartographyAvailability;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(collectionResourceRel = "cartography-availabilities", path = "cartography-availabilities")
public interface CartographyAvailabilityRepository extends CrudRepository<CartographyAvailability, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  CartographyAvailability save(@P("entity") CartographyAvailability entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") CartographyAvailability entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyAvailability','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyAvailability', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<CartographyAvailability> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyAvailability','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyAvailability', 'read')")
  CartographyAvailability findOne(@P("entityId") BigInteger entityId);


}