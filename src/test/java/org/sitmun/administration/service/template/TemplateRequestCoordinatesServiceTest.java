package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TemplateRequestCoordinatesServiceTest {

  private ApplicationRepository applicationRepository;
  private TerritoryRepository territoryRepository;
  private TemplateRequestCoordinatesService service;

  @BeforeEach
  void setUp() {
    applicationRepository = mock(ApplicationRepository.class);
    territoryRepository = mock(TerritoryRepository.class);
    service =
        new TemplateRequestCoordinatesService(
            applicationRepository, territoryRepository, mock(UserRepository.class));
  }

  @Test
  void buildRequiresExistingApplicationAndTerritory() {
    when(applicationRepository.findById(1)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.build(1, 4))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void buildOptionalSkipsMissingApplicationAndStillAttachesTerritory() {
    Territory territory = Territory.builder().id(4).name("Menorca").build();
    when(applicationRepository.findById(1)).thenReturn(Optional.empty());
    when(territoryRepository.findById(4)).thenReturn(Optional.of(territory));

    RequestCoordinates coordinates = service.buildOptional(1, 4);

    assertThat(coordinates.getApplication()).isNull();
    assertThat(coordinates.getTerritory()).isSameAs(territory);
  }

  @Test
  void buildOptionalAttachesApplicationWhenPresent() {
    Application application = Application.builder().id(12).name("IDE Menorca").build();
    Territory territory = Territory.builder().id(4).name("Menorca").build();
    when(applicationRepository.findById(12)).thenReturn(Optional.of(application));
    when(territoryRepository.findById(4)).thenReturn(Optional.of(territory));

    RequestCoordinates coordinates = service.buildOptional(12, 4);

    assertThat(coordinates.getApplication()).isSameAs(application);
    assertThat(coordinates.getTerritory()).isSameAs(territory);
  }
}
