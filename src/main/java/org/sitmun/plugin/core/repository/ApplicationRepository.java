package org.sitmun.plugin.core.repository;


import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import org.sitmun.plugin.core.domain.Application;
import org.sitmun.plugin.core.domain.ApplicationBackground;
import org.sitmun.plugin.core.domain.CartographyGroup;
import org.sitmun.plugin.core.domain.Tree;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;


//@Api(tags = "Applications")
@RepositoryRestResource(collectionResourceRel = "applications", path = "applications"/*, excerptProjection = ApplicationProjection.class*/)
public interface ApplicationRepository extends CrudRepository<Application, BigInteger> {

  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  Application save(@P("entity") Application entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Application entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Application','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Application', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Application> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Application','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Application', 'read')")
  Application findOne(@P("entityId") BigInteger entityId);

  @RestResource(exported = false)
  Optional<Application> findOneByName(String name);

  @RestResource(exported = false)
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select application.trees from Application application where application.id =:id")
  List<Tree> findApplicationTrees(@Param("id") BigInteger id);

  @RestResource(exported = false)
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select application.backgrounds from Application application where application.id =:id")
  List<ApplicationBackground> findApplicationBackgrounds(@Param("id") BigInteger id);

  @RestResource(exported = false)
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select application.situationMap from Application application where application.id =:id")
  List<CartographyGroup> findSituationMap(@Param("id") BigInteger id);


}