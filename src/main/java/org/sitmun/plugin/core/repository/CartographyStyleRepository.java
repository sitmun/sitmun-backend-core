package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.CartographyStyle;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "cartography style")
@RepositoryRestResource(collectionResourceRel = "cartography-styles", path = "cartography-styles")
public interface CartographyStyleRepository
  extends PagingAndSortingRepository<CartographyStyle, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends CartographyStyle> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  @NonNull
  void delete(@P("entity") @NonNull CartographyStyle entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyStyle','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<CartographyStyle> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyStyle','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography', 'read')")
  @NonNull
  Optional<CartographyStyle> findById(@P("entityId") @NonNull Integer entityId);

}