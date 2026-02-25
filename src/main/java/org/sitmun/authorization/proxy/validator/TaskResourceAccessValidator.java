package org.sitmun.authorization.proxy.validator;

import static org.sitmun.domain.DomainConstants.Tasks.PROXY_TYPE_SQL;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.domain.application.ApplicationRepository;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.territory.TerritoryRepository;
import org.sitmun.domain.user.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Validates user access to Task-based resources (SQL tasks, API tasks). Enforces role-based access
 * control by checking if the user has the appropriate role to access the task within the specified
 * application and territory context.
 *
 * <p>Validation includes:
 *
 * <ul>
 *   <li>Task existence validation
 *   <li>User existence validation
 *   <li>Application existence validation
 *   <li>Territory existence validation (when terId > 0)
 *   <li>Role-based access: validates user has roles granting access to the task via task
 *       availabilities and roles
 * </ul>
 *
 * <p>This validator uses the same logic as {@code AuthorizationService.buildProfile()} which calls
 * {@code taskRepository.findByRolesAndTerritory(roles, territoryId)} to ensure consistent
 * permissions. The SQL query guarantees that returned tasks have non-null roles collections through
 * JOIN constraints.
 *
 * <p>This validator is invoked when {@code sitmun.proxy-middleware.validate-user-access=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskResourceAccessValidator implements ResourceAccessValidator {

  private final TaskRepository taskRepository;
  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final TerritoryRepository territoryRepository;

  @Override
  public boolean supports(String type) {
    return PROXY_TYPE_SQL.equalsIgnoreCase(type);
  }

  @Override
  public boolean validate(ConfigProxyRequestDto request, String userName) {
    log.debug(
        "Validating Task access: user={}, appId={}, terId={}, taskId={}",
        userName,
        request.getAppId(),
        request.getTerId(),
        request.getTypeId());

    // For now, implement basic validation - can be enhanced based on business rules
    // Check if task exists
    var taskExists = taskRepository.existsById(request.getTypeId());
    if (!taskExists) {
      log.warn("Task not found: {}", request.getTypeId());
      return false;
    }

    // Check if user exists
    var userOpt = userRepository.findByUsername(userName);
    if (userOpt.isEmpty()) {
      log.warn("User not found: {}", userName);
      return false;
    }

    // Check if application exists
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

    // Role-based access control: Check if user has roles that grant access to this task
    // This follows the same logic as AuthorizationService.buildProfile()
    var user = userOpt.get();
    var userRoles =
        user.getPermissions().stream()
            .filter(
                config ->
                    config.getTerritory() != null
                        && config.getTerritory().getId().equals(request.getTerId()))
            .map(config -> config.getRole())
            .toList();

    if (userRoles.isEmpty()) {
      log.warn(
          "Access denied: User {} has no roles for territory {} - taskId={}",
          userName,
          request.getTerId(),
          request.getTypeId());
      return false;
    }

    // Check if user has access to this task via roles and territory
    // Uses the same query as AuthorizationService: findByRolesAndTerritory
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

    // Role-based access control implemented
    // Task permissions validated via roles and territory availabilities
    log.debug("Task access validation passed for user: {}", userName);
    return true;
  }
}
