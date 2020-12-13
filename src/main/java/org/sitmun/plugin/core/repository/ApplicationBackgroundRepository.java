package org.sitmun.plugin.core.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "application background")
@RepositoryRestResource(collectionResourceRel = "application-backgrounds", path = "application-backgrounds")
public interface ApplicationBackgroundRepository
    extends PagingAndSortingRepository<ApplicationBackground, BigInteger> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends ApplicationBackground> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull ApplicationBackground entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground', 'delete')")
  void deleteById(@P("entityId") @NonNull BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<ApplicationBackground> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.ApplicationBackground', 'read')")
  @NonNull
  Optional<ApplicationBackground> findById(@P("entityId") @NonNull BigInteger entityId);

}