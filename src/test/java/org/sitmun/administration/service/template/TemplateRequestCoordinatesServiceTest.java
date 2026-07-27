package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.task.availability.TaskAvailabilityRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class TemplateRequestCoordinatesServiceTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void buildsExactAuthenticatedProfileCoordinates() {
    ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    TerritoryRepository territoryRepository = mock(TerritoryRepository.class);
    UserRepository userRepository = mock(UserRepository.class);
    Application application = Application.builder().id(7).build();
    Territory territory = Territory.builder().id(11).build();
    User user = User.builder().username("public").build();
    when(applicationRepository.findById(7)).thenReturn(Optional.of(application));
    when(territoryRepository.findById(11)).thenReturn(Optional.of(territory));
    when(userRepository.findByUsername("public")).thenReturn(Optional.of(user));
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("public", null, "ROLE_PUBLIC"));
    TemplateRequestCoordinatesService service =
        new TemplateRequestCoordinatesService(
            applicationRepository,
            mock(TaskAvailabilityRepository.class),
            territoryRepository,
            userRepository);

    var coordinates = service.buildForProfile(7, 11);

    assertThat(coordinates.getApplication()).isSameAs(application);
    assertThat(coordinates.getTerritory()).isSameAs(territory);
    assertThat(coordinates.getUser()).isSameAs(user);
  }
}
