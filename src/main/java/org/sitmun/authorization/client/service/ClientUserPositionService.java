package org.sitmun.authorization.client.service;

import lombok.RequiredArgsConstructor;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionDTO;
import org.sitmun.domain.user.position.UserPositionMapper;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Client-scoped updates for territory user positions owned by the authenticated principal. */
@Service
@RequiredArgsConstructor
public class ClientUserPositionService {

  private final UserApplicationAccessPolicy userApplicationAccessPolicy;
  private final UserPositionRepository userPositionRepository;
  private final UserPositionMapper userPositionMapper;

  @Transactional
  public UserPositionDTO updateOwnedPosition(String username, UserPositionDTO positionDTO) {
    if (positionDTO.getId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UserPosition id is required");
    }
    if (!userApplicationAccessPolicy.mayUseClientConfigEndpoints(username)) {
      throw new AccessDeniedException("Access denied: user account is blocked");
    }

    UserPosition position =
        userPositionRepository
            .findById(positionDTO.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UserPosition not found"));

    if (!username.equals(position.getUser().getUsername())) {
      throw new AccessDeniedException(
          "Access denied: position does not belong to the authenticated user");
    }

    position.setName(positionDTO.getName());
    position.setOrganization(positionDTO.getOrganization());
    position.setEmail(positionDTO.getEmail());
    position.setExpirationDate(positionDTO.getExpirationDate());
    position.setType(positionDTO.getType());

    return userPositionMapper.toDto(userPositionRepository.save(position));
  }
}
