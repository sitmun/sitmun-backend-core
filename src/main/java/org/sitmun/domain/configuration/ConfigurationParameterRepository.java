package org.sitmun.domain.configuration;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "configuration parameter")
@RepositoryRestResource(
    collectionResourceRel = "configuration-parameters",
    path = "configuration-parameters")
public interface ConfigurationParameterRepository
    extends JpaRepository<ConfigurationParameter, Integer> {}
