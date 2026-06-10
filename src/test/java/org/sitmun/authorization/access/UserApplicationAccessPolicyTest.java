package org.sitmun.authorization.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.sitmun.infrastructure.security.core.SecurityConstants.PUBLIC_PRINCIPAL;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserApplicationAccessPolicy")
class UserApplicationAccessPolicyTest {

  private static final int APP_ID = 10;
  private static final String ACTIVE_USERNAME = "activeuser";

  @Mock private UserRepository userRepository;
  @Mock private ApplicationRepository applicationRepository;

  @InjectMocks private UserApplicationAccessPolicy policy;

  @ParameterizedTest(name = "{0}")
  @MethodSource("mayUseClientConfigEndpointsCases")
  @DisplayName("mayUseClientConfigEndpoints — account-level gate")
  void mayUseClientConfigEndpointsMatrix(
      String caseName, String username, boolean userExists, boolean blocked, boolean expected) {
    if (userExists) {
      when(userRepository.findByUsername(username))
          .thenReturn(Optional.of(user(username, blocked)));
    } else {
      when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
    }

    assertThat(policy.mayUseClientConfigEndpoints(username)).isEqualTo(expected);
  }

  static Stream<Arguments> mayUseClientConfigEndpointsCases() {
    return Stream.of(
        Arguments.of("public blocked", PUBLIC_PRINCIPAL, true, true, false),
        Arguments.of("public active", PUBLIC_PRINCIPAL, true, false, true),
        Arguments.of("authenticated blocked", ACTIVE_USERNAME, true, true, false),
        Arguments.of("authenticated active", ACTIVE_USERNAME, true, false, true),
        Arguments.of("unknown user", "ghost", false, false, true));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("mayAccessApplicationCases")
  @DisplayName(
      "mayAccessApplication — public/private app gate (assumes account gate already passed)")
  void mayAccessApplicationMatrix(
      String caseName, String username, boolean appPrivate, boolean expected) {
    if (PUBLIC_PRINCIPAL.equals(username)) {
      when(applicationRepository.findById(APP_ID))
          .thenReturn(Optional.of(Application.builder().id(APP_ID).appPrivate(appPrivate).build()));
    }

    assertThat(policy.mayAccessApplication(APP_ID, username)).isEqualTo(expected);
  }

  static Stream<Arguments> mayAccessApplicationCases() {
    return Stream.of(
        Arguments.of("public on private app", PUBLIC_PRINCIPAL, true, false),
        Arguments.of("public on public app", PUBLIC_PRINCIPAL, false, true),
        Arguments.of("authenticated on private app", ACTIVE_USERNAME, true, true),
        Arguments.of("authenticated on public app", ACTIVE_USERNAME, false, true),
        Arguments.of("unknown user on private app", "ghost", true, true));
  }

  private static User user(String username, boolean blocked) {
    return User.builder().id(1).username(username).blocked(blocked).administrator(false).build();
  }
}
