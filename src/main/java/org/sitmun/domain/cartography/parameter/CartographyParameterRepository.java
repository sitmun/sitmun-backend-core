package org.sitmun.domain.cartography.parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "cartography parameter")
@RepositoryRestResource(
    collectionResourceRel = "cartography-parameters",
    itemResourceRel = "cartography-parameter",
    path = "cartography-parameters")
public interface CartographyParameterRepository
    extends org.springframework.data.jpa.repository.JpaRepository<CartographyParameter, Integer> {}
