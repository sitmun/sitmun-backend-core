package org.sitmun.domain.configuration;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  @RestResource(path = "content", rel = "content")
  @Query(
      """
      select configParam
      from ConfigurationParameter configParam
      where lower(configParam.name) like lower(concat('%', :q, '%'))
      or lower(configParam.value) like lower(concat('%', :q, '%'))
      """)
  Page<ConfigurationParameter> findByContent(@Param("q") String q, Pageable pageable);

  /**
   * Find configuration parameter by name.
   *
   * @param name Parameter name
   * @return Configuration parameter if found
   */
  Optional<ConfigurationParameter> findByName(String name);

  /**
   * Find configuration parameter by name with pessimistic write lock.
   *
   * @param name Parameter name
   * @return Configuration parameter if found
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select cp from ConfigurationParameter cp where cp.name = :name")
  Optional<ConfigurationParameter> findByNameForUpdate(@Param("name") String name);
}
