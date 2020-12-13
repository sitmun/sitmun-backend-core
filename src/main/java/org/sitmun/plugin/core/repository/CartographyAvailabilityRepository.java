package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.CartographyAvailability;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "cartography availability")
@RepositoryRestResource(collectionResourceRel = "cartography-availabilities", path = "cartography-availabilities")
public interface CartographyAvailabilityRepository
    extends PagingAndSortingRepository<CartographyAvailability, BigInteger> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends CartographyAvailability> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  @NonNull
  void delete(@P("entity") @NonNull CartographyAvailability entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyAvailability','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyAvailability', 'delete')")
  void deleteById(@P("entityId") @NonNull BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<CartographyAvailability> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyAvailability','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyAvailability', 'read')")
  @NonNull
  Optional<CartographyAvailability> findById(@P("entityId") @NonNull BigInteger entityId);

}