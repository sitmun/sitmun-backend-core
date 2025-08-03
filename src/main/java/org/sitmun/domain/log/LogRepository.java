package org.sitmun.domain.log;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "log")
@RepositoryRestResource(collectionResourceRel = "logs", path = "logs")
public interface LogRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Log, Integer> {}
