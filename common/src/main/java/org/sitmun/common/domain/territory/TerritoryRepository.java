package org.sitmun.common.domain.territory;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "territory")
@RepositoryRestResource(collectionResourceRel = "territories", path = "territories")
public interface TerritoryRepository extends PagingAndSortingRepository<Territory, Integer> {
}