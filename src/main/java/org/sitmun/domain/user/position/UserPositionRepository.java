package org.sitmun.domain.user.position;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Date;
@Tag(name = "user position")
@RepositoryRestResource(collectionResourceRel = "user-positions", path = "user-positions")
public interface UserPositionRepository extends PagingAndSortingRepository<UserPosition, Integer> {
  @Modifying
  @Transactional
  @Query("UPDATE UserPosition up SET " +
    "up.name = :#{#dto.name}, " +
    "up.organization = :#{#dto.organization}, " +
    "up.email = :#{#dto.email}, " +
    "up.lastModifiedDate = CURRENT_TIMESTAMP, " +
    "up.expirationDate = :#{#dto.expirationDate}, " +
    "up.type = :#{#dto.type} " +
    "WHERE up.id = :id")
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
}