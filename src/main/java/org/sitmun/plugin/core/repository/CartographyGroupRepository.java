package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.CartographyGroup;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "cartography group")
@RepositoryRestResource(collectionResourceRel = "cartography-groups", path = "cartography-groups")
public interface CartographyGroupRepository extends CrudRepository<CartographyGroup, BigInteger> {

  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends CartographyGroup> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") CartographyGroup entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyGroup','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyGroup', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<CartographyGroup> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyGroup','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.CartographyGroup', 'read')")
  Optional<CartographyGroup> findById(@P("entityId") BigInteger entityId);

  @RestResource(exported = false)
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select cartographyGroup.members from CartographyGroup cartographyGroup where cartographyGroup.id =:id")
  List<Cartography> findCartographyMembers(@Param("id") BigInteger id);


}