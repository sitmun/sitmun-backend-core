package org.sitmun.domain.user.position;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.transaction.annotation.Transactional;

@Tag(name = "user position")
@RepositoryRestResource(collectionResourceRel = "user-positions", path = "user-positions")
public interface UserPositionRepository extends JpaRepository<UserPosition, Integer> {

  @Modifying
  @Transactional
  @Query(
      "UPDATE UserPosition up SET "
          + "up.name = :#{#dto.name}, "
          + "up.organization = :#{#dto.organization}, "
          + "up.email = :#{#dto.email}, "
          + "up.lastModifiedDate = CURRENT_TIMESTAMP, "
          + "up.expirationDate = :#{#dto.expirationDate}, "
          + "up.type = :#{#dto.type} "
          + "WHERE up.id = :id")
  void updatePosition(@Param("id") Integer id, @Param("dto") UserPositionDTO dto);

  @Transactional
  default ArrayList<UserPosition> updateBatch(ArrayList<UserPositionDTO> positionDTOs) {
    ArrayList<UserPosition> updatedPositions = new ArrayList<>();
    for (UserPositionDTO dto : positionDTOs) {
      updatePosition(dto.getId(), dto);
      findById(dto.getId()).ifPresent(updatedPositions::add);
    }
    return updatedPositions;
  }

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
}
