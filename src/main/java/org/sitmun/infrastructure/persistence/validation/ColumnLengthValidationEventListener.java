package org.sitmun.infrastructure.persistence.validation;

import org.springframework.data.rest.core.event.AbstractRepositoryEventListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

/**
 * Event listener that validates and truncates string fields before entity persistence. This
 * component intercepts entity creation and update events to ensure that string fields conform to
 * their database column length constraints.
 *
 * <p>The listener is automatically triggered before any entity is created or saved, preventing
 * database constraint violations related to string field lengths.
 *
 * @see ColumnLengthValidator
 */
@Component
public class ColumnLengthValidationEventListener extends AbstractRepositoryEventListener<Object> {

  private final ColumnLengthValidator validator;

  /**
   * Constructs a new event listener with the specified validator.
   *
   * @param validator The validator responsible for checking and truncating string fields
   */
  public ColumnLengthValidationEventListener(ColumnLengthValidator validator) {
    this.validator = validator;
  }

  /**
   * Validates and truncates string fields before entity creation.
   *
   * @param entity The entity being created
   */
  @Override
  protected void onBeforeCreate(Object entity) {
    validateAndTruncate(entity);
  }

  /**
   * Validates and truncates string fields before entity update.
   *
   * @param entity The entity being updated
   */
  @Override
  protected void onBeforeSave(Object entity) {
    validateAndTruncate(entity);
  }

  /**
   * Performs validation and truncation of string fields using the configured validator. Creates a
   * binding result to track validation errors.
   *
   * @param entity The entity to validate and truncate
   */
  private void validateAndTruncate(Object entity) {
    Errors errors = new BeanPropertyBindingResult(entity, entity.getClass().getName());
    validator.validate(entity, errors);
  }
}
