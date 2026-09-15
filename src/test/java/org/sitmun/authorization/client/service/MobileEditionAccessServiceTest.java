package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("Mobile edition access from client-config application list")
class MobileEditionAccessServiceTest {

  @Mock private ApplicationRepository applicationRepository;

  @InjectMocks private MobileEditionAccessService service;

  @Test
  @DisplayName("Allows mobile login when findByUser still lists an ED application")
  void allowsWhenEditionApplicationRemains() {
    Application edition = Application.builder().type("ED").build();
    when(applicationRepository.findByUser(eq("field-user"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(edition)));

    assertThat(service.hasAccessibleEditionApplication("field-user")).isTrue();
  }

  @Test
  @DisplayName("Forbids mobile login when findByUser lists no applications")
  void forbidsWhenEveryGrantIsGone() {
    when(applicationRepository.findByUser(eq("field-user"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    assertThat(service.hasAccessibleEditionApplication("field-user")).isFalse();
  }

  @Test
  @DisplayName("Forbids mobile login when remaining applications are not ED")
  void forbidsWhenOnlyInternalApplicationsRemain() {
    Application internal = Application.builder().type("I").build();
    when(applicationRepository.findByUser(eq("field-user"), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(internal)));

    assertThat(service.hasAccessibleEditionApplication("field-user")).isFalse();
  }
}
