package org.sitmun.domain.tree.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for validating a tree type change against candidate applications.
 *
 * <p>This validation is necessary because Spring Data REST requires two separate PUT operations:
 *
 * <ol>
 *   <li>PUT /api/trees/{id} - updates tree properties (including type)
 *   <li>PUT /api/trees/{id}/availableApplications - updates linked applications
 * </ol>
 *
 * <p>Without proactive validation, the first PUT could succeed (type change) and the second PUT
 * could fail (incompatible applications), requiring a rollback that violates REST principles.
 *
 * <p>This endpoint accepts the intended final state (new type + candidate application IDs) before
 * any changes are committed.
 */
@Getter
@Setter
@Builder
public class TreeTypeValidationRequest {

  /** The intended tree type after the change. Must not be blank. */
  @NotBlank(message = "Tree type is required")
  private String type;

  /**
   * The set of application IDs that will be linked to the tree after the change. If null, defaults
   * to an empty set (no applications).
   */
  @Builder.Default private Set<Integer> applicationIds = Set.of();
}
