package org.sitmun.domain.territory.type;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @deprecated
 */
@Tag(name = "territory group type")
@RepositoryRestResource(collectionResourceRel = "territory-group-types", path = "territory-group-types")
@Deprecated
public interface TerritoryGroupTypeRepository
  extends PagingAndSortingRepository<TerritoryGroupType, Integer> {
}