package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.plugin.core.domain.Application;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.sitmun.plugin.core.domain.CartographyPermission;
import org.sitmun.plugin.core.domain.Tree;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;


@Tag(name = "application")
@RepositoryRestResource(collectionResourceRel = "applications", path = "applications"/*, excerptProjection = ApplicationProjection.class*/)
public interface ApplicationRepository extends PagingAndSortingRepository<Application, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Application> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull Application entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Application','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Application', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<Application> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Application','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Application', 'read')")
  @NonNull
  Optional<Application> findById(@P("entityId") @NonNull Integer entityId);

  @RestResource(exported = false)
  Optional<Application> findOneByName(String name);

  @RestResource(exported = false)
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select application.trees from Application application where application.id =:id")
  List<Tree> findApplicationTrees(@Param("id") Integer id);

  @RestResource(exported = false)
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select application.backgrounds from Application application where application.id =:id")
  List<ApplicationBackground> findApplicationBackgrounds(@Param("id") Integer id);

  @RestResource(exported = false)
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select application.situationMap from Application application where application.id =:id")
  List<CartographyPermission> findSituationMap(@Param("id") Integer id);


}