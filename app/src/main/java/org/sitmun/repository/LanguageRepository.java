package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Language;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Optional;

@Tag(name = "language")
@RepositoryRestResource(collectionResourceRel = "languages", path = "languages")
public interface LanguageRepository
  extends PagingAndSortingRepository<Language, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Language> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull Language entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.Language','administration') or hasPermission(#entityId, 'org.sitmun.domain.Cartography', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<Language> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.Language','administration') or hasPermission(#entityId, 'org.sitmun.domain.Cartography', 'read')")
  @NonNull
  Optional<Language> findById(@P("entityId") @NonNull Integer entityId);

}