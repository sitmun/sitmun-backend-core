package org.sitmun.repository;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.CartographyFilter;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "cartography filter")
@RepositoryRestResource(collectionResourceRel = "cartography-filters", path = "cartography-filters")
public interface CartographyFilterRepository
  extends PagingAndSortingRepository<CartographyFilter, Integer> {
}