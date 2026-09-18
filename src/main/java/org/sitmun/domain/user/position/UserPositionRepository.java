package org.sitmun.domain.user.position;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;

@Tag(name = "user position")
@RepositoryRestResource(collectionResourceRel = "user-positions", path = "user-positions")
public interface UserPositionRepository extends JpaRepository<UserPosition, Integer> {

  /**
   * Find UserPosition by user and territory.
   *
   * @param user the user
   * @param territory the territory
   * @return Optional containing the UserPosition if found
   */
  @RestResource(exported = false)
  @Query("SELECT up FROM UserPosition up WHERE up.user = ?1 AND up.territory = ?2")
  List<UserPosition> findByUserAndTerritory(User user, Territory territory);

  List<UserPosition> findByUser(User user);

  @RestResource(exported = false)
  @Query(
      """
      select (count(pos.id) > 0) from UserPosition pos
      where pos.user.username = ?1
        and (pos.createdDate is null or cast(pos.createdDate as date) <= current_date)
        and (pos.expirationDate is null or current_date <= cast(pos.expirationDate as date))
      """)
  boolean hasAnyActivePosition(String username);
}
