package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.sitmun.test.URIConstants.APPLICATION_URI;
import static org.sitmun.test.URIConstants.APPLICATION_URI_AVAILABLE_ROLES;
import static org.sitmun.test.URIConstants.ROLE_APPLICATIONS_URI;
import static org.sitmun.test.URIConstants.ROLE_URI;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Application role association writes")
class ApplicationRoleAssociationWriteTest {

  @Autowired private MockMvc mvc;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  private final List<Application> applications = new ArrayList<>();
  private final List<Role> roles = new ArrayList<>();

  @AfterEach
  void cleanup() {
    for (Application application : applications) {
      jdbcTemplate.update("DELETE FROM STM_APP_ROL WHERE ARO_APPID = ?", application.getId());
    }
    for (Role role : roles) {
      jdbcTemplate.update("DELETE FROM STM_APP_ROL WHERE ARO_ROLEID = ?", role.getId());
    }
    applications.forEach(application -> applicationRepository.deleteById(application.getId()));
    roles.forEach(role -> roleRepository.deleteById(role.getId()));
    applications.clear();
    roles.clear();
  }

  @Test
  @DisplayName("PUT availableRoles inserts one STM_APP_ROL row visible from both sides")
  @WithMockUser(roles = "ADMIN")
  void putAvailableRolesInsertsOneJoinRowVisibleFromBothSides() throws Exception {
    Application application = application("app");
    Role role = role("role");

    putAvailableRoles(application, role);

    assertThat(joinRows(application.getId(), role.getId())).isEqualTo(1);
    assertAvailableRolesContains(application, role);
    assertApplicationsContains(role, application);
  }

  @Test
  @DisplayName("PUT applications inserts the same STM_APP_ROL pair")
  @WithMockUser(roles = "ADMIN")
  void putApplicationsInsertsTheSamePair() throws Exception {
    Application application = application("app");
    Role role = role("role");

    putApplications(role, application);

    assertThat(joinRows(application.getId(), role.getId())).isEqualTo(1);
    assertAvailableRolesContains(application, role);
    assertApplicationsContains(role, application);
  }

  @Test
  @DisplayName("Replacing availableRoles leaves the applications row")
  @WithMockUser(roles = "ADMIN")
  void replacingAvailableRolesLeavesTheApplicationsRow() throws Exception {
    Application center = application("center");
    Application other = application("other");
    Role childRole = role("child");
    Role replacement = role("next");

    putApplications(childRole, other);
    putAvailableRoles(center, childRole);

    putAvailableRoles(center, replacement);

    assertThat(joinRows(other.getId(), childRole.getId())).isEqualTo(1);
    assertThat(joinRows(center.getId(), replacement.getId())).isEqualTo(1);
    assertThat(joinRows(center.getId(), childRole.getId())).isZero();
    assertApplicationsContains(childRole, other);
    assertAvailableRolesContains(center, replacement);
  }

  @Test
  @DisplayName("Replacing applications leaves the availableRoles row")
  @WithMockUser(roles = "ADMIN")
  void replacingApplicationsLeavesTheAvailableRolesRow() throws Exception {
    Application center = application("center");
    Application replacement = application("next");
    Role childRole = role("child");
    Role parentRole = role("parent");

    putAvailableRoles(center, childRole);
    putApplications(parentRole, center);

    putApplications(parentRole, replacement);

    assertThat(joinRows(center.getId(), childRole.getId())).isEqualTo(1);
    assertThat(joinRows(replacement.getId(), parentRole.getId())).isEqualTo(1);
    assertThat(joinRows(center.getId(), parentRole.getId())).isZero();
    assertAvailableRolesContains(center, childRole);
    assertApplicationsContains(parentRole, replacement);
  }

  private Application application(String label) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Application application =
        applicationRepository.save(
            Application.builder()
                .name(label + "-" + suffix)
                .type("I")
                .jspTemplate("")
                .createdDate(Date.from(Instant.now()))
                .build());
    applications.add(application);
    return application;
  }

  private Role role(String label) {
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    Role role = roleRepository.save(Role.builder().name(label + "-" + suffix).build());
    roles.add(role);
    return role;
  }

  private void putAvailableRoles(Application application, Role role) throws Exception {
    mvc.perform(
            put(APPLICATION_URI_AVAILABLE_ROLES, application.getId())
                .content(ROLE_URI.replace("{0}", role.getId().toString()))
                .contentType("text/uri-list"))
        .andExpect(status().isNoContent());
  }

  private void putApplications(Role role, Application application) throws Exception {
    mvc.perform(
            put(ROLE_APPLICATIONS_URI, role.getId())
                .content(APPLICATION_URI.replace("{0}", application.getId().toString()))
                .contentType("text/uri-list"))
        .andExpect(status().isNoContent());
  }

  private void assertAvailableRolesContains(Application application, Role role) throws Exception {
    mvc.perform(get(APPLICATION_URI_AVAILABLE_ROLES, application.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.roles[*].name", hasItem(role.getName())));
  }

  private void assertApplicationsContains(Role role, Application application) throws Exception {
    mvc.perform(get(ROLE_APPLICATIONS_URI, role.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.applications[*].name", hasItem(application.getName())));
  }

  private Integer joinRows(Integer applicationId, Integer roleId) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM STM_APP_ROL WHERE ARO_APPID = ? AND ARO_ROLEID = ?",
        Integer.class,
        applicationId,
        roleId);
  }
}
