package org.sitmun.domain.database;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "connection")
@RepositoryRestResource(collectionResourceRel = "connections", path = "connections")
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, Integer> {

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select connection
      from DatabaseConnection connection
      where lower(connection.name) like lower(concat('%', :q, '%'))
      or lower(connection.driver) like lower(concat('%', :q, '%'))
      or lower(connection.url) like lower(concat('%', :q, '%'))
      or lower(connection.user) like lower(concat('%', :q, '%'))
      """)
  Page<DatabaseConnection> findByContent(@Param("q") String q, Pageable pageable);
}
