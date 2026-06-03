package org.sitmun.domain.service;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "service")
@RepositoryRestResource(collectionResourceRel = "services", path = "services")
public interface ServiceRepository extends JpaRepository<Service, Integer> {

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select service
      from Service service
      where lower(service.name) like lower(concat('%', :q, '%'))
      or lower(service.serviceURL) like lower(concat('%', :q, '%'))
      or lower(service.type) like lower(concat('%', :q, '%'))
      """)
  Page<Service> findByContent(@Param("q") String q, Pageable pageable);
}
