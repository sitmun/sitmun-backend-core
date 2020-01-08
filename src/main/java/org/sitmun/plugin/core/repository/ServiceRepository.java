package org.sitmun.plugin.core.repository;

import java.math.BigInteger;
import java.util.List;
import org.sitmun.plugin.core.domain.Cartography;
import org.sitmun.plugin.core.domain.Service;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;


@RepositoryRestResource(collectionResourceRel = "services", path = "services")
public interface ServiceRepository extends CrudRepository<Service, BigInteger> {
  @SuppressWarnings("unchecked")
  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity, 'write')")
  Service save(@P("entity") Service entity);

  @Override
  @PreAuthorize("hasPermission(#entity, 'administration') or hasPermission(#entity,  'delete')")
  void delete(@P("entity") Service entity);

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Service','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Service', 'delete')")
  void delete(@P("entityId") BigInteger entityId);

  @Override
  @PostFilter("hasPermission(filterObject, 'administration') or hasPermission(filterObject, 'read')")
  Iterable<Service> findAll();

  @Override
  @PreAuthorize("hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Service','administration') or hasPermission(#entityId, 'org.sitmun.plugin.core.domain.Service', 'read')")
  Service findOne(@P("entityId") BigInteger entityId);
	/*
	@Query("select service from Service service left join fetch service.parameters where service.id =:id")    	
	Service findOneWithEagerRelationships(long id);
	*/

  @RestResource(exported = false)
  @PostFilter("hasPermission(returnObject, 'administration') or hasPermission(filterObject, 'read')")
  @Query("select service.layers from Service service where service.id =:id")
  List<Cartography> findLayers(@Param("id") BigInteger id);


}