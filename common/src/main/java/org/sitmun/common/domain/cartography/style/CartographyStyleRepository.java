package org.sitmun.common.domain.cartography.style;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.common.domain.cartography.Cartography;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "cartography style")
@RepositoryRestResource(collectionResourceRel = "cartography-styles", path = "cartography-styles")
public interface CartographyStyleRepository
  extends PagingAndSortingRepository<CartographyStyle, Integer> {

  @Query("select count(cs) from CartographyStyle cs where cs.defaultStyle = true and cs.cartography = :cartography")
  Integer countDefaultStyles(@Param("cartography") Cartography cartography);

  @Query("select count(cs) from CartographyStyle cs where cs.defaultStyle = true and cs.cartography = :cartography and cs <> :style")
  Integer countDefaultStylesButThis(
    @Param("cartography") Cartography cartography,
    @Param("style") CartographyStyle style);
}