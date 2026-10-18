package org.sitmun.domain.cartography.parameter;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Tag(name = "cartography service parameter")
@RepositoryRestResource(
    collectionResourceRel = "cartography-service-parameters",
    path = "cartography-service-parameters")
public interface CartographyServiceParameterRepository
    extends JpaRepository<CartographyServiceParameter, Integer> {

  boolean existsByCartography_Id(Integer cartographyId);

  boolean existsByCartography_Service_Id(Integer serviceId);

  List<CartographyServiceParameter> findByCartography_Id(Integer cartographyId);

  List<CartographyServiceParameter> findByCartography_Service_Id(Integer serviceId);
}
