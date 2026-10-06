package org.sitmun.domain.service.usage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.access.ServiceAccessSummaries;
import org.sitmun.administration.service.access.ServiceAccessSummary;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.authorization.client.service.ProfileContext;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.availability.CartographyAvailability;
import org.sitmun.domain.cartography.availability.CartographyAvailabilityRepository;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.permission.CartographyPermissionRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.configuration.UserConfiguration;
import org.sitmun.domain.user.configuration.UserConfigurationRepository;
import org.sitmun.domain.user.position.UserPosition;
import org.sitmun.domain.user.position.UserPositionRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.sitmun.proxy.contract.ServiceUsageReport;
import org.sitmun.upstream.signal.ServiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Service usage")
class ServiceUsageTest {

  private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");

  @Autowired private MockMvc mvc;
  @Autowired private ServiceRepository serviceRepository;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private ServiceUsageRepository usageRepository;
  @Autowired private ServiceUsageStore store;
  @Autowired private ServiceUsageQueries queries;
  @Autowired private ServiceAccessSummaries summaries;
  @Autowired private ServiceCheckStore checks;
  @Autowired private ViewerUsage viewerUsage;
  @Autowired private AuthorizationService authorizationService;
  @Autowired private TerritoryRepository territoryRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private UserConfigurationRepository userConfigurationRepository;
  @Autowired private UserPositionRepository userPositionRepository;
  @Autowired private CartographyRepository cartographyRepository;
  @Autowired private CartographyAvailabilityRepository cartographyAvailabilityRepository;
  @Autowired private CartographyPermissionRepository cartographyPermissionRepository;

  @Value("${sitmun.proxy-middleware.secret}")
  private String proxySecret;

  @Value("${sitmun.service-check.usage-retention}")
  private Duration usageRetention;

  @Test
  @DisplayName("Rejects a missing proxy key the same way as service checks")
  void rejectsAMissingProxyKey() throws Exception {
    Service service = saveService("Usage WMS", true);
    Application application = saveApplication("usage-app");

    mvc.perform(
            post("/api/config/proxy/service-usage")
                .contentType(APPLICATION_JSON)
                .content(body(service.getId(), application.getId(), "2026-10-05T12:00:00Z", 3, 1)))
        .andExpect(status().isForbidden());

    assertThat(usageRepository.findByIdServiceIdAndIdOperation(service.getId(), "GetMap"))
        .isEmpty();
  }

  @Test
  @DisplayName("Two posts for the same service, application, day, and operation add")
  void twoPostsAdd() throws Exception {
    Service service = saveService("Counted WMS", true);
    Application application = saveApplication("counted-app");

    mvc.perform(
            post("/api/config/proxy/service-usage")
                .contentType(APPLICATION_JSON)
                .header(SecurityConstants.PROXY_MIDDLEWARE_KEY, proxySecret)
                .content(body(service.getId(), application.getId(), "2026-10-05T12:00:00Z", 3, 1)))
        .andExpect(status().isNoContent());
    mvc.perform(
            post("/api/config/proxy/service-usage")
                .contentType(APPLICATION_JSON)
                .header(SecurityConstants.PROXY_MIDDLEWARE_KEY, proxySecret)
                .content(body(service.getId(), application.getId(), "2026-10-05T12:30:00Z", 4, 2)))
        .andExpect(status().isNoContent());

    ServiceUsage row =
        usageRepository.findByIdServiceIdAndIdOperation(service.getId(), "GetMap").get(0);
    assertThat(row.getRequests()).isEqualTo(7);
    assertThat(row.getFailed()).isEqualTo(3);
    assertThat(row.getId().getApplicationId()).isEqualTo(application.getId());
    assertThat(row.getId().getUsageDay())
        .isEqualTo(
            Instant.parse("2026-10-05T12:00:00Z").atZone(ZoneId.systemDefault()).toLocalDate());
  }

  @Test
  @DisplayName("Hours on either side of local midnight land on two days")
  void midnightSplitsTheLocalDay() {
    Service service = saveService("Midnight WMS", true);
    Application application = saveApplication("midnight-app");
    store.addReport(
        new ServiceUsageReport(
            List.of(
                hour(service.getId(), application.getId(), "2026-10-05T21:00:00Z", 2, 0),
                hour(service.getId(), application.getId(), "2026-10-05T22:00:00Z", 5, 1))),
        MADRID);

    List<ServiceUsage> rows =
        usageRepository.findByIdServiceIdAndIdOperation(service.getId(), "GetMap");
    assertThat(rows)
        .extracting(row -> row.getId().getUsageDay())
        .containsExactlyInAnyOrder(LocalDate.parse("2026-10-05"), LocalDate.parse("2026-10-06"));
    assertThat(rows).extracting(ServiceUsage::getRequests).containsExactlyInAnyOrder(2L, 5L);
  }

  @Test
  @DisplayName(
      "A profile build counts one ViewerConfig per included service and none for a blocked service")
  void profileBuildCountsIncludedServicesOnly() {
    Territory territory =
        territoryRepository.save(
            Territory.builder().name("Usage territory").code("usage-ter").blocked(false).build());
    Role role = roleRepository.save(Role.builder().name("usage-role").build());
    Application application =
        applicationRepository.save(
            Application.builder().name("usage-profile-app").type("I").appPrivate(false).build());
    application.getAvailableRoles().add(role);
    applicationRepository.save(application);
    User user =
        userRepository.save(
            User.builder()
                .administrator(false)
                .blocked(false)
                .firstName("Usage")
                .lastName("User")
                .password("secret")
                .username("usage-user")
                .build());
    userConfigurationRepository.save(
        UserConfiguration.builder()
            .user(user)
            .role(role)
            .territory(territory)
            .appliesToChildrenTerritories(false)
            .build());
    userPositionRepository.save(UserPosition.builder().user(user).territory(territory).build());

    Service included = saveService("Included WMS", true);
    Service blocked = saveService("Blocked WMS", true);
    blocked.setBlocked(true);
    serviceRepository.save(blocked);
    Cartography openLayer = saveLayer("Open layer", included, territory);
    Cartography blockedLayer = saveLayer("Blocked layer", blocked, territory);
    cartographyPermissionRepository.save(
        CartographyPermission.builder()
            .name("usage-permission")
            .type("I")
            .members(new HashSet<>(Set.of(openLayer, blockedLayer)))
            .roles(new HashSet<>(Set.of(role)))
            .build());

    var profile =
        authorizationService.createProfile(
            ProfileContext.builder()
                .username(user.getUsername())
                .appId(application.getId())
                .territoryId(territory.getId())
                .nodeSectionBehaviour(ProfileContext.NodeSectionBehaviour.VIRTUAL_ROOT_ALL_NODES)
                .build());

    assertThat(profile).isPresent();
    viewerUsage.flush();

    assertThat(
            usageRepository.findByIdServiceIdAndIdOperation(
                included.getId(), UsageOperation.VIEWER_CONFIG))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.getRequests()).isEqualTo(1);
              assertThat(row.getId().getApplicationId()).isEqualTo(application.getId());
            });
    assertThat(
            usageRepository.findByIdServiceIdAndIdOperation(
                blocked.getId(), UsageOperation.VIEWER_CONFIG))
        .isEmpty();
  }

  @Test
  @DisplayName("Rows older than usage-retention are deleted")
  void deletesRowsOlderThanRetention() {
    Service service = saveService("Old WMS", true);
    Application application = saveApplication("old-app");
    Instant now = Instant.parse("2026-10-05T12:00:00Z");
    LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
    long days = usageRetention.toDays();
    store.add(service.getId(), application.getId(), today.minusDays(days + 1), "GetMap", 8, 1);
    store.add(service.getId(), application.getId(), today.minusDays(days), "GetMap", 3, 0);

    int deleted = store.deleteExpired(now, usageRetention, ZoneId.systemDefault());

    assertThat(deleted).isEqualTo(1);
    List<ServiceUsage> rows =
        usageRepository.findByIdServiceIdAndIdOperation(service.getId(), "GetMap");
    assertThat(rows).singleElement().extracting(ServiceUsage::getRequests).isEqualTo(3L);
    assertThat(rows.get(0).getId().getUsageDay()).isEqualTo(today.minusDays(days));
  }

  @Test
  @DisplayName("The summary zero-fills, sets measured, and omits request totals when unmeasured")
  void summaryZeroFillsAndOmitsUnmeasuredTotals() {
    Service measured = saveService("Measured WMS", true);
    Service hidden = saveService("Direct WMS", false);
    Application application = saveApplication("summary-app");
    checks.record(
        measured,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("up", 0),
        5L,
        "",
        Instant.parse("2026-10-05T12:00:00Z"));
    checks.record(
        hidden,
        ServiceCheckStore.BACKEND,
        new ServiceStatus("up", 0),
        5L,
        "",
        Instant.parse("2026-10-05T12:00:00Z"));
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    store.add(measured.getId(), application.getId(), today.minusDays(2), "GetMap", 4, 1);
    store.add(measured.getId(), application.getId(), today, UsageOperation.VIEWER_CONFIG, 2, 0);
    store.add(hidden.getId(), application.getId(), today, "GetMap", 9, 9);
    store.add(hidden.getId(), application.getId(), today, UsageOperation.VIEWER_CONFIG, 3, 0);

    List<ServiceAccessSummary> listed = summaries.list(Instant.now());
    UsageWindow measuredWindow = window(listed, measured.getId());
    assertThat(measuredWindow.measured()).isTrue();
    assertThat(measuredWindow.total()).isEqualTo(4);
    assertThat(measuredWindow.failed()).isEqualTo(1);
    assertThat(measuredWindow.viewerLoads()).isEqualTo(2);
    assertThat(measuredWindow.lastUsedDay()).isEqualTo(today.minusDays(2));
    assertThat(measuredWindow.days()).hasSize(30);
    assertThat(measuredWindow.days().get(27)).isEqualTo(4);
    assertThat(measuredWindow.days().stream().filter(value -> value == 0)).hasSize(29);

    UsageWindow hiddenWindow = window(listed, hidden.getId());
    assertThat(hiddenWindow.measured()).isFalse();
    assertThat(hiddenWindow.total()).isNull();
    assertThat(hiddenWindow.failed()).isNull();
    assertThat(hiddenWindow.days()).isNull();
    assertThat(hiddenWindow.viewerLoads()).isEqualTo(3);

    ServiceUsageQueries forcedQueries =
        new ServiceUsageQueries(
            usageRepository,
            serviceRepository,
            applicationRepository,
            true,
            ZoneId.systemDefault());
    UsageWindow forced =
        forcedQueries
            .windows(List.of(hidden.getId()), Map.of(hidden.getId(), false), Instant.now())
            .get(hidden.getId());
    assertThat(forced.measured()).isTrue();
    assertThat(forced.total()).isEqualTo(9);
  }

  @Test
  @DisplayName("Each series point carries failed, and the previous window is the same length")
  void seriesCarriesFailedAndThePreviousWindow() {
    Service measured = saveService("Series WMS", true);
    Service hidden = saveService("Series direct", false);
    Application application = saveApplication("series-app");
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    store.add(measured.getId(), application.getId(), today, "GetMap", 5, 2);
    store.add(measured.getId(), application.getId(), today.minusDays(40), "GetMap", 7, 1);
    store.add(measured.getId(), application.getId(), today.minusDays(100), "GetMap", 4, 3);
    store.add(
        measured.getId(),
        application.getId(),
        today.minusMonths(13).withDayOfMonth(1),
        "GetMap",
        3,
        1);
    store.add(hidden.getId(), application.getId(), today, "GetMap", 9, 4);

    Instant now = Instant.now();
    ServiceUsageView days = queries.view(measured.getId(), "30d", now);
    assertThat(days.asOf()).isEqualTo(now);
    assertThat(days.series()).hasSize(30);
    assertThat(days.total()).isEqualTo(5);
    assertThat(days.series().get(29).failed()).isEqualTo(2);
    assertThat(days.series()).allSatisfy(point -> assertThat(point.failed()).isNotNegative());
    assertThat(days.previousTotal()).isEqualTo(7);
    assertThat(days.previousFailed()).isEqualTo(1);

    ServiceUsageView longer = queries.view(measured.getId(), "90d", Instant.now());
    assertThat(longer.series()).hasSize(90);
    assertThat(longer.previousTotal()).isEqualTo(4);
    assertThat(longer.previousFailed()).isEqualTo(3);

    ServiceUsageView months = queries.view(measured.getId(), "12m", Instant.now());
    assertThat(months.series()).hasSize(12);
    assertThat(months.series()).allSatisfy(point -> assertThat(point.failed()).isNotNegative());
    assertThat(months.previousTotal()).isEqualTo(3);
    assertThat(months.previousFailed()).isEqualTo(1);

    ServiceUsageView unmeasured = queries.view(hidden.getId(), "30d", Instant.now());
    assertThat(unmeasured.measured()).isFalse();
    assertThat(unmeasured.series()).isNull();
    assertThat(unmeasured.previousTotal()).isNull();
    assertThat(unmeasured.previousFailed()).isNull();
  }

  private static UsageWindow window(List<ServiceAccessSummary> listed, Integer serviceId) {
    return listed.stream()
        .filter(summary -> serviceId.equals(summary.serviceId()))
        .findFirst()
        .orElseThrow()
        .usage30();
  }

  private Service saveService(String name, boolean proxied) {
    return serviceRepository.save(
        Service.builder()
            .name(name)
            .type("WMS")
            .serviceURL("http://maps.example/wms")
            .blocked(false)
            .isProxied(proxied)
            .build());
  }

  private Application saveApplication(String name) {
    return applicationRepository.save(Application.builder().name(name).type("I").build());
  }

  private Cartography saveLayer(String name, Service service, Territory territory) {
    Cartography layer =
        cartographyRepository.save(
            Cartography.builder()
                .name(name)
                .service(service)
                .layers(List.of("L1"))
                .blocked(false)
                .queryableFeatureAvailable(false)
                .queryableFeatureEnabled(false)
                .build());
    CartographyAvailability availability = new CartographyAvailability();
    availability.setCartography(layer);
    availability.setTerritory(territory);
    availability.setCreatedDate(Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)));
    cartographyAvailabilityRepository.save(availability);
    return layer;
  }

  private static String body(
      Integer serviceId, Integer applicationId, String hour, long requests, long failed) {
    return "[" + hourJson(serviceId, applicationId, hour, requests, failed) + "]";
  }

  private static String hourJson(
      Integer serviceId, Integer applicationId, String hour, long requests, long failed) {
    return """
        {
          "serviceId": %d,
          "applicationId": %d,
          "hourStart": "%s",
          "operation": "GetMap",
          "requests": %d,
          "failed": %d
        }
        """
        .formatted(serviceId, applicationId, hour, requests, failed);
  }

  private static org.sitmun.proxy.contract.ServiceUsage hour(
      Integer serviceId, Integer applicationId, String hour, long requests, long failed) {
    return new org.sitmun.proxy.contract.ServiceUsage(
        serviceId, applicationId, Instant.parse(hour), "GetMap", requests, failed);
  }
}
