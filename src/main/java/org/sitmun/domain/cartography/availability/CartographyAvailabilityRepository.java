package org.sitmun.domain.cartography.availability;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "cartography availability")
@RepositoryRestResource(
    collectionResourceRel = "cartography-availabilities",
    path = "cartography-availabilities")
public interface CartographyAvailabilityRepository
    extends org.springframework.data.jpa.repository.JpaRepository<
        CartographyAvailability, Integer> {}
