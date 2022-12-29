package org.sitmun.common.domain.cartography.parameter;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @deprecated
 */
@Tag(name = "cartography spatial selection parameter")
@RepositoryRestResource(
  collectionResourceRel = "cartography-spatial-selection-parameters",
  itemResourceRel = "cartography-spatial-selection-parameter",
  path = "cartography-spatial-selection-parameters")
@Deprecated
public interface CartographySpatialSelectionParameterRepository
  extends PagingAndSortingRepository<CartographySpatialSelectionParameter, Integer> {
}