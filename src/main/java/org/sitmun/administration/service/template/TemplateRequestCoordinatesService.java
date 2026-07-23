package org.sitmun.administration.service.template;

import lombok.RequiredArgsConstructor;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TemplateRequestCoordinatesService {

  private final ApplicationRepository applicationRepository;
  private final TerritoryRepository territoryRepository;
  private final UserRepository userRepository;

  /** Preview-only: user principal for backend variables; no app/territory required. */
  public RequestCoordinates buildForCurrentUser() {
    RequestCoordinates coordinates = new RequestCoordinates();
    attachCurrentUser(coordinates);
    return coordinates;
  }

  public RequestCoordinates build(Integer appId, Integer terId) {
    if (appId == null || terId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "appId and terId are required");
    }

    Application application =
        applicationRepository
            .findById(appId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    Territory territory =
        territoryRepository
            .findById(terId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Territory not found"));

    RequestCoordinates coordinates = new RequestCoordinates();
    coordinates.setApplication(application);
    coordinates.setTerritory(territory);
    attachCurrentUser(coordinates);
    return coordinates;
  }

  private void attachCurrentUser(RequestCoordinates coordinates) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getName() != null) {
      userRepository.findByUsername(authentication.getName()).ifPresent(coordinates::setUser);
    }
  }
}
