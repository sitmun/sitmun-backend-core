package org.sitmun.repository;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.TerritoryGroupType;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "territory group type")
@RepositoryRestResource(collectionResourceRel = "territory-group-types", path = "territory-group-types")
@Deprecated
public interface TerritoryGroupTypeRepository
  extends PagingAndSortingRepository<TerritoryGroupType, Integer> {
}