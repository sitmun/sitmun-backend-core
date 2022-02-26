package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.ConfigurationParameter;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "configuration parameter")
@RepositoryRestResource(collectionResourceRel = "configuration-parameters", path = "configuration-parameters")
public interface ConfigurationParameterRepository
  extends PagingAndSortingRepository<ConfigurationParameter, Integer> {
}