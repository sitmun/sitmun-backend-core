package org.sitmun.domain.application;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExternalApplicationUrlValidator")
class ExternalApplicationUrlValidatorTest {

  private static final String IDEE_URL = "https://www.idee.es";

  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  @DisplayName("type E with valid https URL passes")
  void externalAppWithValidUrlPasses() {
    Application app = Application.builder().name("IDEE").type("E").jspTemplate(IDEE_URL).build();

    Set<ConstraintViolation<Application>> violations = validator.validate(app);

    assertThat(violations).isEmpty();
  }

  @Test
  @DisplayName("type E with blank jspTemplate fails on jspTemplate")
  void externalAppWithBlankUrlFails() {
    Application app = Application.builder().name("External").type("E").jspTemplate(" ").build();

    Set<ConstraintViolation<Application>> violations = validator.validate(app);

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("jspTemplate");
  }

  @Test
  @DisplayName("type E with non-http scheme fails")
  void externalAppWithInvalidSchemeFails() {
    Application app =
        Application.builder().name("External").type("E").jspTemplate("ftp://example.com").build();

    Set<ConstraintViolation<Application>> violations = validator.validate(app);

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("jspTemplate");
  }

  @Test
  @DisplayName("type I with blank jspTemplate passes")
  void internalAppWithBlankTemplatePasses() {
    Application app = Application.builder().name("Internal").type("I").jspTemplate(null).build();

    Set<ConstraintViolation<Application>> violations = validator.validate(app);

    assertThat(violations).isEmpty();
  }
}
