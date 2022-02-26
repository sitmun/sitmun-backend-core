package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.CartographyParameter;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "cartography parameter")
@RepositoryRestResource(
  collectionResourceRel = "cartography-parameters",
  itemResourceRel = "cartography-parameter",
  path = "cartography-parameters")
public interface CartographyParameterRepository
  extends PagingAndSortingRepository<CartographyParameter, Integer> {
}