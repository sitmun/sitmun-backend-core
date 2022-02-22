package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.Log;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "log")
@RepositoryRestResource(collectionResourceRel = "logs", path = "logs")
public interface LogRepository extends PagingAndSortingRepository<Log, Integer> {
}