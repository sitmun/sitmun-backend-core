package org.sitmun.domain.user.position;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

import java.util.Optional;

@Tag(name = "user position")
@RepositoryRestResource(collectionResourceRel = "user-positions", path = "user-positions")
public interface UserPositionRepository extends
  PagingAndSortingRepository<UserPosition, Integer> {

  /**
   * Find UserPosition by user and territory.
   *
   * @param user the user
   * @param territory the territory
   * @return Optional containing the UserPosition if found
   */
  @RestResource(exported = false)
  @Query("SELECT up FROM UserPosition up WHERE up.user = ?1 AND up.territory = ?2")
  Optional<UserPosition> findByUserAndTerritory(User user, Territory territory);
}