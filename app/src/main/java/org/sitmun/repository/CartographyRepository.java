package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Cartography;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

import java.util.Date;
import java.util.Optional;

@Tag(name = "cartography")
@RepositoryRestResource(
  collectionResourceRel = "cartographies",
  itemResourceRel = "cartography",
  path = "cartographies")
public interface CartographyRepository extends PagingAndSortingRepository<Cartography, Integer> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  @NonNull
  <S extends Cartography> S save(@P("entity") @NonNull S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") @NonNull Cartography entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.Cartography','administration') or hasPermission(#entityId, 'org.sitmun.domain.Cartography', 'delete')")
  void deleteById(@P("entityId") @NonNull Integer entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  @NonNull
  Iterable<Cartography> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.domain.Cartography','administration') or hasPermission(#entityId, 'org.sitmun.domain.Cartography', 'read')")
  @NonNull
  Optional<Cartography> findById(@P("entityId") @NonNull Integer entityId);

  @Query("select cartography from Cartography cartography left join fetch cartography.service where cartography.id =:id")
  Cartography findOneWithEagerRelationships(Integer id);

  @Query(name = "dashboard.cartographiesByCreatedDate")
  Iterable<Object[]> cartographiesByCreatedDateSinceDate(@Param("sinceDate") Date sinceDate);

  @Query("select distinct cartography from Cartography cartography, Application app, Role role, CartographyPermission permission " +
    "where app.id = :applicationId and role member of app.availableRoles and role member of permission.roles and permission member of cartography.permissions")
  Iterable<Cartography> available(@P("applicationId") @NonNull Integer applicationId);
}