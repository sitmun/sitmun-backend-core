package org.sitmun.plugin.core.constraints;

import org.sitmun.plugin.core.repository.CodeListValueRepository;
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
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value != null) {
      return codeRepository.existsByCodeListNameAndValue(codeList, value);
    } else {
      return true;
    }
  }
}
