package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
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
            applicationRepository, territoryRepository, userRepository);

    var coordinates = service.build(7, 11);

    assertThat(coordinates.getApplication()).isSameAs(application);
    assertThat(coordinates.getTerritory()).isSameAs(territory);
    assertThat(coordinates.getUser()).isSameAs(user);
  }

  @Test
  void buildRequiresExistingApplicationAndTerritory() {
    ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    TerritoryRepository territoryRepository = mock(TerritoryRepository.class);
    when(applicationRepository.findById(1)).thenReturn(Optional.empty());
    TemplateRequestCoordinatesService service =
        new TemplateRequestCoordinatesService(
            applicationRepository, territoryRepository, mock(UserRepository.class));

    assertThatThrownBy(() -> service.build(1, 4))
        .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
  }

  @Test
  void buildOptionalSkipsMissingApplicationAndStillAttachesTerritory() {
    Territory territory = Territory.builder().id(4).name("Menorca").build();
    ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    TerritoryRepository territoryRepository = mock(TerritoryRepository.class);
    when(applicationRepository.findById(1)).thenReturn(Optional.empty());
    when(territoryRepository.findById(4)).thenReturn(Optional.of(territory));
    TemplateRequestCoordinatesService service =
        new TemplateRequestCoordinatesService(
            applicationRepository, territoryRepository, mock(UserRepository.class));

    RequestCoordinates coordinates = service.buildOptional(1, 4);

    assertThat(coordinates.getApplication()).isNull();
    assertThat(coordinates.getTerritory()).isSameAs(territory);
  }

  @Test
  void buildOptionalAttachesApplicationWhenPresent() {
    Application application = Application.builder().id(12).name("IDE Menorca").build();
    Territory territory = Territory.builder().id(4).name("Menorca").build();
    ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
    TerritoryRepository territoryRepository = mock(TerritoryRepository.class);
    when(applicationRepository.findById(12)).thenReturn(Optional.of(application));
    when(territoryRepository.findById(4)).thenReturn(Optional.of(territory));
    TemplateRequestCoordinatesService service =
        new TemplateRequestCoordinatesService(
            applicationRepository, territoryRepository, mock(UserRepository.class));

    RequestCoordinates coordinates = service.buildOptional(12, 4);

    assertThat(coordinates.getApplication()).isSameAs(application);
    assertThat(coordinates.getTerritory()).isSameAs(territory);
  }
}
