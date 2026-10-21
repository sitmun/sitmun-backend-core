package org.sitmun.authorization.proxy.validator;

import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Validates user access to Task-based resources (SQL tasks, API tasks, URL redirects). Enforces
 * role-based access control by checking if the user has the appropriate role to access the task
 * within the specified application and territory context.
 *
 * <p>Supported types: SQL, API, URL (more-info task scope types)
 *
 * <p>Validation includes:
 *
 * <ul>
 *   <li>Task existence validation
 *   <li>User existence validation
 *   <li>Application existence validation
 *   <li>Territory existence validation (when terId > 0)
 *   <li>Role-based access: the user must hold a role of this application in the territory, and that
 *       role must grant the task there
 * </ul>
 *
 * <p>Roles are {@code RoleRepository.findRolesByApplicationAndUserAndTerritory}, the same link the
 * client profile uses. Accessible tasks are then {@code taskRepository.findByRolesAndTerritory}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResourceAccessValidator implements ResourceAccessValidator {

  // Task-based proxy types (more-info scope types)
  private static final Set<String> SUPPORTED_TYPES = Set.of(SCOPE_SQL, SCOPE_API, SCOPE_URL);

  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final TerritoryRepository territoryRepository;
  private final RoleRepository roleRepository;

  @Override
  public boolean supports(String type) {
    return SUPPORTED_TYPES.stream().anyMatch(t -> t.equalsIgnoreCase(type));
  }

  @Override
  public boolean validate(ConfigProxyRequestDto request, String userName) {
    log.debug(
        "Validating Task access: user={}, appId={}, terId={}, taskId={}",
        userName,
        request.getAppId(),
        request.getTerId(),
        request.getTypeId());

    var taskExists = taskRepository.existsById(request.getTypeId());
    if (!taskExists) {
      log.warn("Task not found: {}", request.getTypeId());
      return false;
    }

    var userOpt = userRepository.findByUsername(userName);
    if (userOpt.isEmpty()) {
      log.warn("User not found: {}", userName);
      return false;
    }

    var applicationExists = applicationRepository.existsById(request.getAppId());
    if (!applicationExists) {
      log.warn("Application not found: {}", request.getAppId());
      return false;
    }

    // Check if territory exists (terId can be 0 for public access)
    if (request.getTerId() > 0) {
      var territoryExists = territoryRepository.existsById(request.getTerId());
      if (!territoryExists) {
        log.warn("Territory not found: {}", request.getTerId());
        return false;
      }
    }

    var userRoles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            userName, request.getAppId(), request.getTerId());

    if (userRoles.isEmpty()) {
      log.warn(
          "Access denied: User {} has no role of application {} in territory {} - taskId={}",
          userName,
          request.getAppId(),
          request.getTerId(),
          request.getTypeId());
      return false;
    }

    var accessibleTasks = taskRepository.findByRolesAndTerritory(userRoles, request.getTerId());
    var hasAccess =
        accessibleTasks.stream().anyMatch(task -> task.getId().equals(request.getTypeId()));

    if (!hasAccess) {
      log.warn(
          "Access denied: User {} has no permissions for task {} in territory {}",
          userName,
          request.getTypeId(),
          request.getTerId());
      return false;
    }

    log.debug("Task access validation passed for user: {}", userName);
    return true;
  }
}
