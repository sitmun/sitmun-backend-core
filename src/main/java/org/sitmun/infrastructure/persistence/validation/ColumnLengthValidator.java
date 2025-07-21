package org.sitmun.infrastructure.persistence.validation;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * A Spring Validator that automatically truncates string fields to their maximum length as defined
 * by {@code @Column(length)} annotation.
 *
 * <p>This validator performs two main operations:
 *
 * <ol>
 *   <li>Validates the target object using standard validators through {@code
 *       LocalValidatorFactoryBean}.
 *   <li>Truncates any string fields that exceed their {@code @Column(length}) constraint.
 * </ol>
 */
@Component
@Slf4j
public class ColumnLengthValidator implements Validator {

  private final LocalValidatorFactoryBean validatorFactory;

  /**
   * Constructs a new ColumnLengthValidator with the specified validator factory.
   *
   * @param validatorFactory The Spring validator factory used for standard validation
   * @throws IllegalArgumentException if validatorFactory is null
   */
  public ColumnLengthValidator(LocalValidatorFactoryBean validatorFactory) {
    this.validatorFactory = validatorFactory;
  }

  /**
   * Indicates whether this validator supports validation of the given class. This implementation
   * supports all classes.
   *
   * @param clazz The class to check for validation support
   * @return true for all classes
   * @throws IllegalArgumentException if clazz is null
   */
  @Override
  public boolean supports(@NotNull Class<?> clazz) {
    return true;
  }

  /**
   * Validates the target object by:
   *
   * <ol>
   *   <li>Running standard validations through the validator factory
   *   <li>Truncating any string fields that exceed their {@code @Column(length)} constraint
   * </ol>
   *
   * @param target The object to validate
   * @param errors The {@code Errors} object to store validation errors
   * @throws IllegalArgumentException if target or errors is null
   */
  @Override
  public void validate(@NotNull Object target, @NotNull Errors errors) {
    // First validate using standard validators
    validatorFactory.validate(target, errors);

    // Then handle string truncation using Spring's BeanWrapper
    BeanWrapper beanWrapper = new BeanWrapperImpl(target);
    PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();

    for (PropertyDescriptor descriptor : propertyDescriptors) {
      if (String.class.equals(descriptor.getPropertyType())) {
        String propertyName = descriptor.getName();
        getField(target.getClass(), propertyName)
            .ifPresent(getFieldConsumer(beanWrapper, propertyName));
      }
    }
  }

  /**
   * Creates a consumer that handles field truncation based on {@code @Column} annotation.
   *
   * @param beanWrapper The Spring BeanWrapper for property access
   * @param propertyName The name of the property to process
   * @return A consumer that truncates the field if needed
   * @throws IllegalArgumentException if beanWrapper or propertyName is null
   */
  private static @NotNull Consumer<Field> getFieldConsumer(
      BeanWrapper beanWrapper, String propertyName) {
    return field -> {
      Lob lob = field.getAnnotation(Lob.class);
      Column column = field.getAnnotation(Column.class);
      if (lob == null && column != null && column.length() > 0) {
        String value = (String) beanWrapper.getPropertyValue(propertyName);
        if (value != null && value.length() > column.length()) {
          beanWrapper.setPropertyValue(propertyName, value.substring(0, column.length()));
          log.info(
              "Truncated field {} from {} to {} characters",
              propertyName,
              value.length(),
              column.length());
        }
      }
    };
  }

  /**
   * Recursively searches for a field in the class hierarchy.
   *
   * @param clazz The class to search in
   * @param fieldName The name of the field to find
   * @return Optional containing the field if found, empty otherwise
   * @throws IllegalArgumentException if clazz or fieldName is null
   */
  private Optional<Field> getField(Class<?> clazz, String fieldName) {
    try {
      return Optional.of(clazz.getDeclaredField(fieldName));
    } catch (NoSuchFieldException e) {
      Class<?> superClass = clazz.getSuperclass();
      if (superClass != null) {
        return getField(superClass, fieldName);
      }
      return Optional.empty();
    }
  }
}
