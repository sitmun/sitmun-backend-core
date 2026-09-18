package org.sitmun.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ServiceEventHandler")
class ServiceEventHandlerTest {

  private final ServiceEventHandler handler = new ServiceEventHandler();

  @Test
  @DisplayName("Keeps stored password when PUT password is null")
  void keepsStoredPasswordOnNullPut() {
    Service service =
        Service.builder().blocked(false).password("kept").authenticationMode("None").build();
    service.postLoad();
    service.setPassword(null);

    handler.handleBeforeSave(service);

    assertThat(service.getPassword()).isEqualTo("kept");
  }

  @Test
  @DisplayName("Clears password when PUT password is empty")
  void clearsPasswordOnEmptyPut() {
    Service service =
        Service.builder().blocked(false).password("secret").authenticationMode("None").build();
    service.postLoad();
    service.setPassword("");

    handler.handleBeforeSave(service);

    assertThat(service.getPassword()).isNull();
  }

  @Test
  @DisplayName("Forces proxied when authentication is not None")
  void forcesProxiedWhenAuthenticated() {
    Service service =
        Service.builder()
            .blocked(false)
            .isProxied(false)
            .authenticationMode("HTTP Basic authentication")
            .build();

    handler.handleBeforeSave(service);

    assertThat(service.getIsProxied()).isTrue();
  }

  @Test
  @DisplayName("Does not force proxied off when authentication is None")
  void doesNotForceProxiedOffWhenNone() {
    Service service =
        Service.builder().blocked(false).isProxied(true).authenticationMode("None").build();

    handler.handleBeforeSave(service);

    assertThat(service.getIsProxied()).isTrue();
  }

  @Test
  @DisplayName("Create treats empty password as null and forces proxied for Basic")
  void createClearsEmptyPasswordAndForcesProxied() {
    Service service =
        Service.builder()
            .blocked(false)
            .isProxied(false)
            .authenticationMode("HTTP Basic authentication")
            .password("")
            .build();

    handler.handleBeforeCreate(service);

    assertThat(service.getPassword()).isNull();
    assertThat(service.getIsProxied()).isTrue();
  }
}
