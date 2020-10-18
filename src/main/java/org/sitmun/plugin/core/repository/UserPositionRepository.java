package org.sitmun.plugin.core.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigInteger;
import java.util.Optional;
import org.sitmun.plugin.core.domain.UserPosition;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;

@Tag(name = "user position")
@RepositoryRestResource(collectionResourceRel = "user-positions", path = "user-positions")
public interface UserPositionRepository extends CrudRepository<UserPosition, BigInteger> {

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  <S extends UserPosition> S save(@P("entity") S entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") UserPosition entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.UserPosition', 'delete')")
  void deleteById(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(#entity, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<UserPosition> findAll();

  @Override
  @PostAuthorize("hasPermission(#entity, 'administration') or hasPermission(returnObject, 'read')")
  Optional<UserPosition> findById(BigInteger id);
}