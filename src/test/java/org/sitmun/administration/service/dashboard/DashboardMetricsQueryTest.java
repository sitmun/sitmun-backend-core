package org.sitmun.administration.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EnableConfigurationProperties(DashboardProperties.class)
@DisplayName("Dashboard metric queries")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DashboardMetricsQueryTest {

  @Autowired private EntityManager entityManager;
  @Autowired private DashboardProperties dashboardProperties;
  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private TerritoryTypeRepository territoryTypeRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private UserConfigurationRepository userConfigurationRepository;
  @Autowired private UserPositionRepository userPositionRepository;

  @Test
  @DisplayName("Users created on a day are bucketed by year, month, and day")
  void publishesDayBucketForCreatedDate() {
    User user = persistUser("dash-day");
    Date created =
        Date.from(
            LocalDate.of(2011, 4, 9).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant());
    user.setCreatedDate(created);
    user = userRepository.saveAndFlush(user);
    Date persisted = userRepository.findById(user.getId()).orElseThrow().getCreatedDate();
    LocalDate bucket = persisted.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    List<Object[]> rows = run(dashboardProperties.getUsersByCreatedDate().getQuery());

    assertThat(rows)
        .filteredOn(row -> row.length == 4 && row[0] != null && row[1] != null && row[2] != null)
        .anySatisfy(
            row -> {
              assertThat(number(row[0])).isEqualTo(bucket.getYear());
              assertThat(number(row[1])).isEqualTo(bucket.getMonthValue());
              assertThat(number(row[2])).isEqualTo(bucket.getDayOfMonth());
              assertThat(number(row[3])).isGreaterThanOrEqualTo(1);
            });
  }

  @Test
  @DisplayName("Counts a direct configuration with an open position and omits an expired one")
  void countsOnlyActiveDirectConfiguration() {
    Territory territory = persistTerritory();
    Role role = persistRole();
    Application application = persistApplication(role, false, false);
    User active = persistUser("dash-active");
    User expired = persistUser("dash-expired");
    persistConfiguration(active, territory, role, false);
    persistConfiguration(expired, territory, role, false);
    persistPosition(active, territory, null, atStartOfDay(1));
    persistPosition(expired, territory, null, atStartOfDay(-1));

    assertThat(countFor(application.getName())).isEqualTo(1);
  }

  @Test
  @DisplayName("Omits a children configuration when the application has neither entry flag")
  void omitsChildrenConfigurationWithoutEntryFlags() {
    Territory territory = persistTerritory();
    Role role = persistRole();
    Application application = persistApplication(role, false, false);
    User user = persistUser("dash-children");
    persistConfiguration(user, territory, role, true);
    persistPosition(user, territory, null, null);

    assertThat(countFor(application.getName())).isZero();
  }

  @Test
  @DisplayName("Counts a children configuration when the application allows the parent territory")
  void countsChildrenConfigurationWhenParentAccessAllowed() {
    Territory territory = persistTerritory();
    Role role = persistRole();
    Application application = persistApplication(role, true, false);
    User user = persistUser("dash-parent");
    persistConfiguration(user, territory, role, true);
    persistPosition(user, territory, null, null);

    assertThat(countFor(application.getName())).isEqualTo(1);
  }

  @Test
  @DisplayName("Counts built-in public with a direct role and no position")
  void countsBuiltInPublicWithoutPosition() {
    User publicUser =
        userRepository.findByUsername(SecurityConstants.PUBLIC_PRINCIPAL).orElseThrow();
    Territory territory = persistTerritory();
    Role role = persistRole();
    Application application = persistApplication(role, false, false);
    persistConfiguration(publicUser, territory, role, false);

    assertThat(countFor(application.getName())).isEqualTo(1);
  }

  @SuppressWarnings("unchecked")
  private List<Object[]> run(String jpql) {
    entityManager.flush();
    return entityManager.createQuery(jpql).getResultList();
  }

  private long countFor(String applicationName) {
    return run(dashboardProperties.getUsersPerApplication().getQuery()).stream()
        .filter(row -> applicationName.equals(String.valueOf(row[0])))
        .map(row -> ((Number) row[1]).longValue())
        .findFirst()
        .orElse(0L);
  }

  private static long number(Object value) {
    return ((Number) value).longValue();
  }

  private User persistUser(String prefix) {
    User user = new User();
    user.setUsername(prefix + "-" + UUID.randomUUID().toString().substring(0, 8));
    user.setPassword("password");
    user.setFirstName("Test");
    user.setLastName("User");
    user.setAdministrator(false);
    user.setBlocked(false);
    user.setEmail(prefix + "@example.com");
    return userRepository.save(user);
  }

  private Territory persistTerritory() {
    TerritoryType type =
        territoryTypeRepository.save(
            TerritoryType.builder()
                .name("type-" + UUID.randomUUID())
                .official(false)
                .topType(false)
                .bottomType(false)
                .build());
    String id = "ter-" + UUID.randomUUID();
    return territoryRepository.save(
        Territory.builder()
            .name(id)
            .code(id.substring(0, 16))
            .blocked(false)
            .territorialAuthorityEmail("admin@example.com")
            .createdDate(new Date())
            .territorialAuthorityName("Test Authority")
            .type(type)
            .build());
  }

  private Role persistRole() {
    return roleRepository.save(
        Role.builder().name("role-" + UUID.randomUUID()).description("test").build());
  }

  private Application persistApplication(Role role, boolean accessParent, boolean accessChildren) {
    Set<Role> roles = new HashSet<>();
    roles.add(role);
    return applicationRepository.save(
        Application.builder()
            .name("app-" + UUID.randomUUID())
            .type("I")
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

  private void persistPosition(
      User user, Territory territory, Date createdDate, Date expirationDate) {
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

  @TestConfiguration
  @Import(I18nTestConfiguration.class)
  static class Configuration {}
}
