package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.sitmun.plugin.core.domain.Translation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "translation")
@RepositoryRestResource(collectionResourceRel = "translations", path = "translations")
public interface TranslationRepository
    extends CrudRepository<Translation, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends Translation> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Translation entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Translation','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography', 'delete')")
  void deleteById(@P("entityId") Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Translation> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Translation','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Cartography', 'read')")
  Optional<Translation> findById(@P("entityId") Integer entityId);

}