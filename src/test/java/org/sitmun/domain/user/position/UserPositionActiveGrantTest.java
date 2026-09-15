package org.sitmun.domain.user.position;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.territory.type.TerritoryType;
import org.sitmun.domain.territory.type.TerritoryTypeRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("UserPosition active grant interval")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserPositionActiveGrantTest {

  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private TerritoryTypeRepository territoryTypeRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private UserConfigurationRepository userConfigurationRepository;
  @Autowired private UserPositionRepository userPositionRepository;

  @Test
  @DisplayName("Restricted user omits expired grant and keeps live grant")
  void omitsExpiredGrantAndKeepsLiveGrant() {
    Fixture fx = persistTwoTerritories();
    persistPosition(fx.user, fx.live, null, atStartOfDay(1));
    persistPosition(fx.user, fx.expired, null, atStartOfDay(-1));

    var listed =
        territoryRepository.findByRestrictedUser(fx.user.getUsername(), PageRequest.of(0, 20));
    assertThat(listed.getContent()).extracting(Territory::getId).containsExactly(fx.live.getId());

    var appsLive =
        applicationRepository.findByRestrictedUserAndTerritory(
            fx.user.getUsername(), fx.live.getId(), PageRequest.of(0, 20));
    assertThat(appsLive.getContent())
        .extracting(Application::getId)
        .contains(fx.application.getId());

    var appsExpired =
        applicationRepository.findByRestrictedUserAndTerritory(
            fx.user.getUsername(), fx.expired.getId(), PageRequest.of(0, 20));
    assertThat(appsExpired).isEmpty();

    var profileLive =
        applicationRepository.findByRestrictedUserApplicationAndTerritory(
            fx.user.getUsername(), fx.application.getId(), fx.live.getId());
    assertThat(profileLive).isPresent();

    var profileExpired =
        applicationRepository.findByRestrictedUserApplicationAndTerritory(
            fx.user.getUsername(), fx.application.getId(), fx.expired.getId());
    assertThat(profileExpired).isEmpty();

    var rolesLive =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            fx.user.getUsername(), fx.application.getId(), fx.live.getId());
    assertThat(rolesLive).extracting(Role::getId).contains(fx.role.getId());

    var rolesExpired =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            fx.user.getUsername(), fx.application.getId(), fx.expired.getId());
    assertThat(rolesExpired).isEmpty();
  }

  @Test
  @DisplayName("expirationDate equal to today is listed; next calendar day is omitted")
  void expirationTodayInclusiveNextDayOmitted() {
    Fixture fx = persistTwoTerritories();
    persistPosition(fx.user, fx.live, null, atStartOfDay(0));
    persistPosition(fx.user, fx.expired, null, atStartOfDay(-1));

    var listed =
        territoryRepository.findByRestrictedUser(fx.user.getUsername(), PageRequest.of(0, 20));
    assertThat(listed.getContent()).extracting(Territory::getId).containsExactly(fx.live.getId());
  }

  @Test
  @DisplayName("Null createdDate is open left; future createdDate is omitted")
  void nullCreatedDateOpenLeftFutureCreatedDateOmitted() {
    Fixture fx = persistTwoTerritories();
    persistPosition(fx.user, fx.live, null, null);
    persistPosition(fx.user, fx.expired, atStartOfDay(1), null);

    var listed =
        territoryRepository.findByRestrictedUser(fx.user.getUsername(), PageRequest.of(0, 20));
    assertThat(listed.getContent()).extracting(Territory::getId).containsExactly(fx.live.getId());
  }

  @Test
  @DisplayName("Active parent still expands children without their own STM_POST")
  void activeParentExpandsChildrenWithoutOwnPosition() {
    Fixture fx = persistParentChild();
    persistPosition(fx.user, fx.live, null, null);

    var listed =
        territoryRepository.findByRestrictedUser(fx.user.getUsername(), PageRequest.of(0, 20));
    assertThat(listed.getContent())
        .extracting(Territory::getId)
        .contains(fx.live.getId(), fx.expired.getId());
  }

  @Test
  @DisplayName("findByUser omits the application when every grant is expired")
  void findByUserOmitsApplicationWhenEveryGrantExpired() {
    Fixture fx = persistTwoTerritories();
    persistPosition(fx.user, fx.live, null, atStartOfDay(-1));
    persistPosition(fx.user, fx.expired, null, atStartOfDay(-1));

    var listed = applicationRepository.findByUser(fx.user.getUsername(), PageRequest.of(0, 20));
    assertThat(listed.getContent())
        .extracting(Application::getId)
        .doesNotContain(fx.application.getId());
  }

  @Test
  @DisplayName("Built-in public still lists territories with no positions")
  void publicListsWithoutPositions() {
    var territories =
        territoryRepository.findByRestrictedUser(
            SecurityConstants.PUBLIC_PRINCIPAL, PageRequest.of(0, 10));
    assertThat(territories.getTotalElements()).isEqualTo(3);
  }

  @Test
  @DisplayName("Built-in admin still lists applications with no positions")
  void adminListsWithoutPositions() {
    var applications =
        applicationRepository.findByUser(
            SecurityConstants.BUILT_IN_ADMIN_PRINCIPAL, PageRequest.of(0, 10));
    assertThat(applications.getTotalElements()).isGreaterThan(0);
  }

  @Test
  @DisplayName(
      "hasAnyActivePosition is true for a live cargo and false when every cargo is expired")
  void hasAnyActivePositionMatchesInterval() {
    Fixture fx = persistTwoTerritories();
    persistPosition(fx.user, fx.live, null, atStartOfDay(1));
    persistPosition(fx.user, fx.expired, null, atStartOfDay(-1));
    assertThat(userPositionRepository.hasAnyActivePosition(fx.user.getUsername())).isTrue();

    userPositionRepository
        .findByUser(fx.user)
        .forEach(
            position -> {
              position.setExpirationDate(atStartOfDay(-1));
              userPositionRepository.save(position);
            });
    assertThat(userPositionRepository.hasAnyActivePosition(fx.user.getUsername())).isFalse();
  }

  private Fixture persistTwoTerritories() {
    TerritoryType type = persistType();
    Territory live = persistTerritory(type, "live");
    Territory expired = persistTerritory(type, "expired");
    User user = persistUser();
    Role role = persistRole();
    Application application = persistApplication(role, false, false, "I");
    persistConfiguration(user, live, role, false);
    persistConfiguration(user, expired, role, false);
    return new Fixture(user, live, expired, role, application);
  }

  private Fixture persistParentChild() {
    TerritoryType type = persistType();
    Territory parent = persistTerritory(type, "parent");
    Territory child = persistTerritory(type, "child");
    parent.getMembers().add(child);
    parent = territoryRepository.save(parent);
    User user = persistUser();
    Role role = persistRole();
    Application application = persistApplication(role, true, true, "I");
    persistConfiguration(user, parent, role, true);
    return new Fixture(user, parent, child, role, application);
  }

  private TerritoryType persistType() {
    return territoryTypeRepository.save(
        TerritoryType.builder()
            .name("type-" + UUID.randomUUID())
            .official(false)
            .topType(false)
            .bottomType(false)
            .build());
  }

  private Territory persistTerritory(TerritoryType type, String suffix) {
    String id = suffix + "-" + UUID.randomUUID();
    return territoryRepository.save(
        Territory.builder()
            .name(id)
            .code(id.substring(0, Math.min(20, id.length())))
            .blocked(false)
            .territorialAuthorityEmail("admin@example.com")
            .createdDate(new Date())
            .territorialAuthorityName("Test Authority")
            .type(type)
            .build());
  }

  private User persistUser() {
    User user = new User();
    user.setUsername("upag-" + UUID.randomUUID().toString().substring(0, 12));
    user.setPassword("password");
    user.setFirstName("Test");
    user.setLastName("User");
    user.setAdministrator(false);
    user.setBlocked(false);
    user.setEmail("upag@example.com");
    return userRepository.save(user);
  }

  private Role persistRole() {
    return roleRepository.save(
        Role.builder().name("role-" + UUID.randomUUID()).description("test").build());
  }

  private Application persistApplication(
      Role role, boolean accessParent, boolean accessChildren, String type) {
    Set<Role> roles = new HashSet<>();
    roles.add(role);
    return applicationRepository.save(
        Application.builder()
            .name("app-" + UUID.randomUUID())
            .type(type)
            .title("test")
            .createdDate(new Date())
            .lastUpdate(new Date())
            .appPrivate(true)
            .accessParentTerritory(accessParent)
            .accessChildrenTerritory(accessChildren)
            .availableRoles(roles)
            .build());
  }

  private void persistConfiguration(User user, Territory territory, Role role, boolean children) {
    userConfigurationRepository.save(
        UserConfiguration.builder()
            .user(user)
            .territory(territory)
            .role(role)
            .appliesToChildrenTerritories(children)
            .build());
  }

  private void persistPosition(User user, Territory territory, Date createdDate, Date expirationDate) {
    UserPosition position =
        UserPosition.builder()
            .user(user)
            .territory(territory)
            .name("cargo")
            .organization("org")
            .createdDate(createdDate)
            .expirationDate(expirationDate)
            .build();
    position = userPositionRepository.save(position);
    position.setCreatedDate(createdDate);
    position.setExpirationDate(expirationDate);
    userPositionRepository.save(position);
  }

  private static Date atStartOfDay(int dayOffset) {
    return Date.from(
        LocalDate.now(ZoneId.systemDefault())
            .plusDays(dayOffset)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant());
  }

  private record Fixture(
      User user, Territory live, Territory expired, Role role, Application application) {}

  @TestConfiguration
  @Import(I18nTestConfiguration.class)
  static class Configuration {}
}
