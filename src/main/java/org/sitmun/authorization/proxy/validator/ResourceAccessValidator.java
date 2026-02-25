package org.sitmun.authorization.proxy.validator;

import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;

/**
 * Strategy interface for validating user access to different resource types. Implementations should
 * verify that a user has permission to access a specific resource (Task, Service, etc.) within the
 * context of an application and territory.
 */
public interface ResourceAccessValidator {

  /**
   * Checks if this validator can handle the given resource type.
   *
   * @param type the resource type (e.g., "SQL", "WMS", "WMTS", "API")
   * @return true if this validator can validate access for the given type
   */
  boolean supports(String type);

  /**
   * Validates if the user has access to the requested resource.
   *
   * @param request the proxy configuration request containing appId, terId, type, and typeId
   * @param userName the username to validate access for
   * @return true if the user has access, false otherwise
   */
  boolean validate(ConfigProxyRequestDto request, String userName);
}
