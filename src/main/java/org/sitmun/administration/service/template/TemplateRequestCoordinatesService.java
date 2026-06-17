package org.sitmun.administration.service.template;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.task.availability.TaskAvailabilityRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TemplateRequestCoordinatesService {

  private final ApplicationRepository applicationRepository;
  private final TaskAvailabilityRepository taskAvailabilityRepository;
  private final UserRepository userRepository;

  public RequestCoordinates build(Integer templateTaskId) {
    RequestCoordinates coordinates = new RequestCoordinates();
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getName() != null) {
      userRepository.findByUsername(authentication.getName()).ifPresent(coordinates::setUser);
    }

    if (templateTaskId == null) {
      return coordinates;
    }

    List<Territory> territories =
        taskAvailabilityRepository.findByTaskId(templateTaskId).stream()
            .map(taskAvailability -> taskAvailability.getTerritory())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (territories.size() == 1) {
      coordinates.setTerritory(territories.get(0));
    }

    List<Application> applications = applicationRepository.findByTaskId(templateTaskId);
    Application application = null;
    if (!applications.isEmpty()) {
      application =
          applications.stream()
              .sorted((left, right) -> left.getId().compareTo(right.getId()))
              .findFirst()
              .orElse(null);
    } else {
      application =
          applicationRepository.findAll(PageRequest.of(0, 1, Sort.by("id"))).stream()
              .findFirst()
              .orElse(null);
    }

    if (application != null) {
      coordinates.setApplication(application);
      if (coordinates.getTerritory() == null
          && application.getTerritories() != null
          && application.getTerritories().size() == 1) {
        coordinates.setTerritory(application.getTerritories().iterator().next().getTerritory());
      }
    }

    return coordinates;
  }
}
