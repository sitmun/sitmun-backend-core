package org.sitmun.authorization.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.authorization.service.AuthorizationService;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.role.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("Authorization Service test")
class AuthorizationServiceTest {

  private @Autowired AuthorizationService authorizationService;

  @Test
  @DisplayName("Application given identifiers of user, territory and application")
  void application() {
    Optional<Application> app = authorizationService.findApplicationByIdAndUserAndTerritory("public", 1, 1);
    assertTrue(app.isPresent());
    app.ifPresent(application -> assertThat(application.getName()).isEqualTo("SITMUN - Provincial"));
  }

  @Test
  @DisplayName("Roles given identifiers of user, territory and application")
  void roles() {
    List<Role> roles = authorizationService.findRolesByApplicationAndUserAndTerritory("public",1, 1);
    assertThat(roles).asList().hasSize(1);
    assertThat(roles.get(0).getName()).isEqualTo("Role 1");
  }

}
