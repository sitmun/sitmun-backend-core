package org.sitmun.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sitmun.domain.application.Application;

class DomainConstantsApplicationTypeTest {

  @ParameterizedTest
  @CsvSource({"T,true", "t,true", "ED,false", "E,false", "I,false"})
  void isTouristicApplicationMatchesTypeCode(String type, boolean expected) {
    Application app = Application.builder().type(type).build();
    assertThat(DomainConstants.Applications.isTouristicApplication(app)).isEqualTo(expected);
  }

  @Test
  void isTouristicApplicationIsFalseForNull() {
    assertThat(DomainConstants.Applications.isTouristicApplication(null)).isFalse();
    assertThat(DomainConstants.Applications.isTouristicApplication(Application.builder().build()))
        .isFalse();
  }
}
