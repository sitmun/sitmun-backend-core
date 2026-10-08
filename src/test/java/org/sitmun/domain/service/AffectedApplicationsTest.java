package org.sitmun.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.application.background.ApplicationBackground;
import org.sitmun.domain.application.background.ApplicationBackgroundRepository;
import org.sitmun.domain.application.tree.ApplicationTree;
import org.sitmun.domain.application.tree.ApplicationTreeRepository;
import org.sitmun.domain.background.Background;
import org.sitmun.domain.background.BackgroundRepository;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.permission.CartographyPermissionRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.sitmun.domain.service.usage.ServiceUsage;
import org.sitmun.domain.service.usage.ServiceUsageKey;
import org.sitmun.domain.service.usage.ServiceUsageRepository;
import org.sitmun.domain.service.usage.UsageOperation;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.tree.Tree;
import org.sitmun.domain.tree.TreeRepository;
import org.sitmun.domain.tree.node.TreeNode;
import org.sitmun.domain.tree.node.TreeNodeRepository;
import org.sitmun.infrastructure.persistence.type.i18n.I18nTestConfiguration;
import org.sitmun.upstream.signal.ServiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ImportAutoConfiguration(LiquibaseAutoConfiguration.class)
@Transactional
@DisplayName("Affected applications")
class AffectedApplicationsTest {

  private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

  private String token;

  @Autowired private EntityManager entities;
  @Autowired private AffectedApplications affected;
  @Autowired private ServiceRepository services;
  @Autowired private ServiceCheckStore checks;
  @Autowired private ServiceUsageRepository usage;
  @Autowired private ApplicationRepository applications;
  @Autowired private TreeRepository trees;
  @Autowired private TreeNodeRepository nodes;
  @Autowired private CartographyRepository layers;
  @Autowired private ApplicationTreeRepository applicationTrees;
  @Autowired private CartographyPermissionRepository permissions;
  @Autowired private BackgroundRepository backgrounds;
  @Autowired private ApplicationBackgroundRepository applicationBackgrounds;
  @Autowired private TaskRepository tasks;
  @Autowired private RoleRepository roles;

  @BeforeEach
  void token() {
    token = "A" + java.util.UUID.randomUUID().toString().substring(0, 8);
  }

  @Test
  @DisplayName(
      "usage on a failing service ranks above a configured application, and healthy or unused services add none")
  void ranksUsageAboveConfiguration() {
    Service roads = failing("Roads");
    Service lonely = failing("Lonely");
    Service healthy = save("Healthy", 0);
    Application configured = application("Configured");
    Application used = application("Used");
    Application healthyOnly = application("Healthy only");
    Application viewerOnly = application("Viewer only");
    placeOnTree(configured, layer(roads, null));
    LocalDate today = NOW.atZone(ZoneId.systemDefault()).toLocalDate();
    recordUsage(roads, used, today.minusDays(29), UsageOperation.GET_MAP, 4);
    recordUsage(roads, used, today.minusDays(30), UsageOperation.GET_MAP, 100);
    recordUsage(healthy, healthyOnly, today, UsageOperation.GET_MAP, 9);
    placeOnTree(healthyOnly, layer(healthy, null));
    recordUsage(roads, viewerOnly, today, UsageOperation.VIEWER_CONFIG, 6);

    entities.flush();
    List<AffectedApplication> rows = ours();

    assertThat(rows)
        .containsExactly(
            row(used, 4, AffectedApplication.AffectedUsage.used, roads),
            row(configured, 0, AffectedApplication.AffectedUsage.configured, roads));
    assertThat(affected.list(NOW))
        .extracting(AffectedApplication::applicationName)
        .doesNotContain(healthyOnly.getName(), viewerOnly.getName());
    assertThat(affected.list(NOW))
        .flatExtracting(AffectedApplication::failingServices)
        .extracting(AffectedApplication.FailingService::id)
        .doesNotContain(lonely.getId(), healthy.getId());
  }

  @Test
  @DisplayName("each configuration path includes the application once, and a shared role does not")
  void configurationPaths() {
    Service roads = failing("Roads");
    Application spatial = application("Spatial");
    Application background = application("Background");
    Application situation = application("Situation");
    Application taskApp = application("Task");
    Application roleOnly = application("Role only");
    Application both = application("Both");
    placeOnTree(spatial, layer(save("Portrayal", 0), roads));
    placeOnBackground(background, layer(roads, null));
    placeOnSituationMap(situation, layer(roads, null));
    placeTask(taskApp, roads);
    shareRoleOnly(roleOnly, roads);
    placeOnTree(both, layer(roads, null));
    placeOnBackground(both, layer(roads, null));
    recordUsage(
        roads, both, NOW.atZone(ZoneId.systemDefault()).toLocalDate(), UsageOperation.GET_MAP, 3);

    entities.flush();

    assertThat(ours())
        .containsExactly(
            row(both, 3, AffectedApplication.AffectedUsage.used, roads),
            row(background, 0, AffectedApplication.AffectedUsage.configured, roads),
            row(situation, 0, AffectedApplication.AffectedUsage.configured, roads),
            row(spatial, 0, AffectedApplication.AffectedUsage.configured, roads),
            row(taskApp, 0, AffectedApplication.AffectedUsage.configured, roads));
    assertThat(affected.list(NOW))
        .extracting(AffectedApplication::applicationName)
        .doesNotContain(roleOnly.getName());
  }

  private List<AffectedApplication> ours() {
    return affected.list(NOW).stream()
        .filter(row -> row.applicationName().startsWith(token))
        .toList();
  }

  private AffectedApplication row(
      Application application,
      long requests,
      AffectedApplication.AffectedUsage usage,
      Service... failing) {
    List<AffectedApplication.FailingService> services =
        java.util.Arrays.stream(failing)
            .map(
                service ->
                    new AffectedApplication.FailingService(service.getId(), service.getName()))
            .toList();
    return new AffectedApplication(
        application.getId(), application.getName(), services, requests, usage);
  }

  private Service failing(String name) {
    return save(name, 70);
  }

  private Service save(String name, int rank) {
    Service service =
        services.save(
            Service.builder()
                .name(token + " " + name)
                .type("WMS")
                .serviceURL("http://example.test/" + name)
                .blocked(false)
                .isProxied(false)
                .build());
    checks.record(
        service, ServiceCheckStore.BACKEND, new ServiceStatus("probe", rank), 10L, "", NOW);
    return service;
  }

  private Application application(String name) {
    return applications.save(
        Application.builder()
            .name(token + " " + name)
            .type("I")
            .createdDate(Date.from(NOW))
            .lastUpdate(Date.from(NOW))
            .build());
  }

  private Cartography layer(Service portrayal, Service spatialSelection) {
    return layers.save(
        Cartography.builder()
            .name(token + " layer " + portrayal.getId())
            .service(portrayal)
            .spatialSelectionService(spatialSelection)
            .layers(List.of("L1"))
            .blocked(false)
            .queryableFeatureAvailable(false)
            .queryableFeatureEnabled(false)
            .build());
  }

  private void placeOnTree(Application application, Cartography layer) {
    Tree tree = trees.save(Tree.builder().name(token + " tree " + application.getId()).build());
    applicationTrees.save(ApplicationTree.builder().application(application).tree(tree).build());
    nodes.save(TreeNode.builder().name(token + " node").tree(tree).cartography(layer).build());
  }

  private void placeOnBackground(Application application, Cartography layer) {
    CartographyPermission permission =
        permissions.save(
            CartographyPermission.builder()
                .name(token + " bg " + application.getId())
                .type("F")
                .members(new HashSet<>(Set.of(layer)))
                .build());
    Background background =
        backgrounds.save(
            Background.builder()
                .name(token + " background " + application.getId())
                .cartographyGroup(permission)
                .build());
    applicationBackgrounds.save(
        ApplicationBackground.builder().application(application).background(background).build());
  }

  private void placeOnSituationMap(Application application, Cartography layer) {
    CartographyPermission permission =
        permissions.save(
            CartographyPermission.builder()
                .name(token + " situation " + application.getId())
                .type("M")
                .members(new HashSet<>(Set.of(layer)))
                .build());
    application.setSituationMap(permission);
    applications.save(application);
  }

  private void placeTask(Application application, Service service) {
    Task task =
        tasks.save(
            Task.builder().name(token + " task " + application.getId()).service(service).build());
    Tree tree =
        trees.save(Tree.builder().name(token + " task tree " + application.getId()).build());
    applicationTrees.save(ApplicationTree.builder().application(application).tree(tree).build());
    nodes.save(TreeNode.builder().name(token + " task node").tree(tree).task(task).build());
  }

  private void shareRoleOnly(Application application, Service service) {
    Role role = roles.save(Role.builder().name(token + " role").build());
    application.getAvailableRoles().add(role);
    applications.save(application);
    Task task = tasks.save(Task.builder().name(token + " role task").service(service).build());
    task.getRoles().add(role);
    tasks.save(task);
  }

  private void recordUsage(
      Service service, Application application, LocalDate day, String operation, long requests) {
    ServiceUsage row = new ServiceUsage();
    row.setId(new ServiceUsageKey(service.getId(), application.getId(), day, operation));
    row.setRequests(requests);
    usage.save(row);
  }

  @TestConfiguration
  @Import({I18nTestConfiguration.class, ServiceCheckStore.class, AffectedApplications.class})
  static class Configuration {}
}
