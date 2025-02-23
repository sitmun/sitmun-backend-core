package org.sitmun.authorization.service;

import com.google.common.collect.ImmutableList;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.cartography.CartographyRepository;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.domain.cartography.permission.CartographyPermissionRepository;
import org.sitmun.domain.configuration.ConfigurationParameter;
import org.sitmun.domain.configuration.ConfigurationParameterRepository;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.territory.TerritoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

  private final TerritoryRepository territoryRepository;
  private final ApplicationRepository applicationRepository;
  private final RoleRepository roleRepository;
  private final CartographyPermissionRepository cartographyPermissionRepository;
  private final CartographyRepository cartographyRepository;
  private final ConfigurationParameterRepository configurationParameterRepository;
  private final TaskRepository taskRepository;

  public AuthorizationService(ApplicationRepository applicationRepository,
                              TerritoryRepository territoryRepository, RoleRepository roleRepository,
                              ConfigurationParameterRepository configurationParameterRepository,
                              CartographyPermissionRepository cartographyPermissionRepository,
                              CartographyRepository cartographyRepository,
                              TaskRepository taskRepository) {
    this.applicationRepository = applicationRepository;
    this.territoryRepository = territoryRepository;
    this.roleRepository = roleRepository;
    this.configurationParameterRepository = configurationParameterRepository;
    this.cartographyPermissionRepository = cartographyPermissionRepository;
    this.cartographyRepository = cartographyRepository;
    this.taskRepository = taskRepository;
  }

  /**
   * The list of applications for a user.
   * The logic is as follows:
   * <ul>
   *   <li>From the application, we can discover the roles (ROLE).</li>
   *   <li>We match all user configuration where (USER, *, ROLE, *)</li>
   * </ul>
   */
  public Page<Application> findApplicationsByUser(String username, Pageable pageable) {
    return applicationRepository.findByUser(username, pageable);
  }

  /**
   * The list of territories for a user.
   * The logic is as follows:
   * <ul>
   *   <li>We match all user configuration where (USER, *, ROLE, *)</li>
   *   <li>From the territory, we can discover the roles (USER, TERRITORY, *, FALSE).</li>
   *   <li>When a user configuration is (USER, TERRITORY, *, TRUE) we consider included the application if {@link Application#getAccessParentTerritory()} is `true`</li>
   *   <li>When a parent territory matches (USER, TERRITORY-PARENT, *, TRUE) we consider included the application if {@link Application#getAccessChildrenTerritory()} ()} is `true`</li>
   *   <li>And ensure that the territory included is related to the application of the role</li>
   * </ul>
   */
  public Page<Territory> findTerritoriesByUser(String username, Pageable pageable) {
    return territoryRepository.findByUser(username, pageable);
  }


  /**
   * Get the list of territories for the user in a given application.
   * The logic is as follows:
   * <ul>
   *   <li>From the application, we can discover the roles (ROLE).</li>
   *   <li>We match all user configuration where (USER, territory, ROLE, *)</li>
   *   <li>When a user configuration is (USER, territory, ROLE, TRUE) we consider included the territory if {@link Application#getAccessParentTerritory()} is `true`</li>
   * </ul>
   */
  public Page<Territory> findTerritoriesByUserAndApplication(String username, Integer appId, Pageable pageable) {
    return territoryRepository.findByUserAndApplication(username, appId, pageable);
  }

  /**
   * Get the list of applications for the user in a given territory.
   * The logic is as follows:
   * <ul>
   *   <li>We match all user configurations where (USER, TERRITORY, *, false)</li>
   *   <li>When a user configuration is (USER, TERRITORY, *, TRUE) we consider included the territory if {@link Application#getAccessParentTerritory()} is `true`</li>
   * </ul>
   */
  public Page<Application> findApplicationsByUserAndTerritory(String username, Integer territoryId, Pageable pageable) {
    return applicationRepository.findByUserAndTerritory(username, territoryId, pageable);
  }

  /**
   * Refina la lista de aplicaciones restringiendo a una única aplicación.
   */
  public Optional<Application> findApplicationByIdAndUserAndTerritory(String username, Integer appId, Integer territoryId) {
    return applicationRepository.findByIdAndUserAndTerritory(username, appId, territoryId);
  }

  public List<Role> findRolesByApplicationAndUserAndTerritory(String username, Integer appId, Integer territoryId) {
    return roleRepository.findRolesByApplicationAndUserAndTerritory(username, appId, territoryId);
  }

  public Optional<Profile> createProfile(String username, Integer appId, Integer territoryId) {
    Optional<Application> application = findApplicationByIdAndUserAndTerritory(username, appId, territoryId);
    if (application.isEmpty()) {
      return Optional.empty();
    }
    Optional<Territory> territory = territoryRepository.findById(territoryId);
    if (territory.isEmpty()) {
      return Optional.empty();
    }

    List<Role> roles = roleRepository.findRolesByApplicationAndUserAndTerritory(username, appId, territoryId);
    List<CartographyPermission> cartographyPermissions = cartographyPermissionRepository.findByRolesAndTerritory(roles, territoryId);
    List<Cartography> layers = cartographyRepository.findByRolesAndTerritory(roles, territoryId);
    List<Task> tasks = taskRepository.findByRolesAndTerritory(roles, territoryId);
    List<ConfigurationParameter> global = ImmutableList.copyOf(configurationParameterRepository.findAll());

    List<org.sitmun.domain.service.Service> services = new ArrayList<>();

    layers.forEach(layer -> services.add(layer.getService()));
    tasks.forEach(task -> services.add(task.getService()));
    List<org.sitmun.domain.service.Service> filteredServices = services.stream().filter(Objects::nonNull).filter(distinctByKey(org.sitmun.domain.service.Service::getId)).collect(Collectors.toList());

    return Optional.of(Profile.builder()
      .application(application.get())
      .territory(territory.get())
      .groups(cartographyPermissions)
      .layers(layers)
      .tasks(tasks)
      .services(filteredServices)
      .global(global)
      .build());
  }

  public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
  }
}
