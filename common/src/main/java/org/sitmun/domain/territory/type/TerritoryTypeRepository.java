package org.sitmun.domain.territory.type;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "territory type")
@RepositoryRestResource(collectionResourceRel = "territory-types", path = "territory-types")
public interface TerritoryTypeRepository extends
  PagingAndSortingRepository<TerritoryType, Integer> {
}