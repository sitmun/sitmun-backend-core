package org.sitmun.domain.application;

import java.util.Objects;
import org.sitmun.domain.user.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Validates creator changes against the persisted APP_CREATORID. Allows preserving an existing
 * invalid creator on unrelated edits; rejects only new/replacement ineligible creators.
 */
@Component
public class ApplicationPointOfContactValidation {

  private final JdbcTemplate jdbcTemplate;

  public ApplicationPointOfContactValidation(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Returns true when the creator change is valid:
   *
   * <ul>
   *   <li>Clearing to null is always allowed.
   *   <li>Preserving the existing persisted creator is always allowed (even if now ineligible).
   *   <li>Setting a new/different eligible creator is allowed.
   *   <li>Setting a new/different ineligible creator is rejected.
   * </ul>
   */
  public boolean isValidCreatorChange(Integer applicationId, User incomingCreator) {
    if (incomingCreator == null) {
      return true;
    }
    Integer persistedCreatorId = fetchPersistedCreatorId(applicationId);
    if (Objects.equals(persistedCreatorId, incomingCreator.getId())) {
      return true;
    }
    return ApplicationPointOfContactPolicy.isEligible(incomingCreator);
  }

  private Integer fetchPersistedCreatorId(Integer applicationId) {
    if (applicationId == null) {
      return null;
    }
    return jdbcTemplate.queryForObject(
        "SELECT APP_CREATORID FROM STM_APP WHERE APP_ID = ?", Integer.class, applicationId);
  }
}
