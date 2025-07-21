package org.sitmun.infrastructure.persistence.type.codelist;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class CodeListValidator implements ConstraintValidator<CodeList, String>, ApplicationContextAware {

  private String codeList;
  private ApplicationContext applicationContext;

  @Override
  public void initialize(CodeList constraintAnnotation) {
    codeList = constraintAnnotation.value();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    if (value != null && applicationContext != null) {
      CodeListValueRepository codeRepository = applicationContext.getBean(CodeListValueRepository.class);
      return codeRepository.existsByCodeListNameAndValue(codeList, value);
    }
    return true;
  }
}
