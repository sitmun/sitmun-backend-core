package org.sitmun.authentication.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.sitmun.authentication.dto.UserPasswordAuthenticationRequest;
import org.sitmun.authentication.service.CookieService;
import org.sitmun.authorization.client.service.MobileEditionAccessService;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.infrastructure.security.core.Rfc9457ResponseWriter;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.infrastructure.web.dto.ProblemDetail;
import org.sitmun.infrastructure.web.dto.ProblemTypes;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;

class AuthenticationControllerFailedLoginTest {

  @Test
  void returnsGenericProblemDetailWhenAuthenticationIsNotAccepted() {
    var authenticationManager = mock(AuthenticationManager.class);
    var authentication = mock(Authentication.class);
    when(authenticationManager.authenticate(any())).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(false);
    var controller =
        new AuthenticationController(
            authenticationManager,
            mock(UserDetailsService.class),
            mock(JsonWebTokenService.class),
            mock(UserRepository.class),
            mock(CookieService.class),
            new Rfc9457ResponseWriter(new ObjectMapper()),
            mock(MobileEditionAccessService.class));
    var request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/authenticate");
    var login = new UserPasswordAuthenticationRequest();
    login.setUsername("user");
    login.setPassword("wrong");

    var response = controller.authenticateUser(login, request, mock(HttpServletResponse.class));

    assertThat(response.getStatusCode().value()).isEqualTo(401);
    Object body = response.getBody();
    assertThat(body).isInstanceOf(ProblemDetail.class);
    var problem = (ProblemDetail) body;
    assertThat(problem.getType()).isEqualTo(ProblemTypes.UNAUTHORIZED);
    assertThat(problem.getStatus()).isEqualTo(401);
    assertThat(problem.getTitle()).isEqualTo("Unauthorized");
    assertThat(problem.getDetail()).isEqualTo("Authentication is required");
    assertThat(problem.getInstance()).isEqualTo("/api/authenticate");
  }
}
