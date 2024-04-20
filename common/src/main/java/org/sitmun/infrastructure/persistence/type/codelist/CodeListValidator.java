package org.sitmun.infrastructure.persistence.type.codelist;

import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CodeListValidator implements ConstraintValidator<CodeList, String> {

  private String codeList;

  @Autowired
  private CodeListValueRepository codeRepository;

  @Override
  public void initialize(CodeList constraintAnnotation) {
    codeList = constraintAnnotation.value();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
    if (value != null) {
      return codeRepository.existsByCodeListNameAndValue(codeList, value);
    } else {
      return true;
    }
  }
}
