package org.sitmun.domain.database;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "connection")
@RepositoryRestResource(collectionResourceRel = "connections", path = "connections")
public interface DatabaseConnectionRepository extends PagingAndSortingRepository<DatabaseConnection, Integer> {
}