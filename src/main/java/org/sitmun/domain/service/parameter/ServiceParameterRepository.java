package org.sitmun.domain.service.parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "service parameter")
@RepositoryRestResource(collectionResourceRel = "service-parameters", path = "service-parameters")
public interface ServiceParameterRepository extends JpaRepository<ServiceParameter, Integer> {}
