package org.sitmun.domain.configuration;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "configuration parameter")
@RepositoryRestResource(
    collectionResourceRel = "configuration-parameters",
    path = "configuration-parameters")
public interface ConfigurationParameterRepository
    extends JpaRepository<ConfigurationParameter, Integer> {

  Optional<ConfigurationParameter> findByName(String name);

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select configParam
      from ConfigurationParameter configParam
      where lower(configParam.name) like lower(concat('%', :q, '%'))
      or lower(configParam.value) like lower(concat('%', :q, '%'))
      """)
  Page<ConfigurationParameter> findByContent(@Param("q") String q, Pageable pageable);
}
