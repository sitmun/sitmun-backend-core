package org.sitmun.domain.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.application.parameter.ApplicationParameter;

@DisplayName("ConfigurationParameter")
class ConfigurationParameterTest {

  @Test
  @DisplayName("parameters with the same id are equal and collapse in a set")
  void sameIdAreEqual() {
    ConfigurationParameter left =
        ConfigurationParameter.builder().id(7).name("language.default").value("ca").build();
    ConfigurationParameter right =
        ConfigurationParameter.builder().id(7).name("other").value("es").build();

    assertThat(left).isEqualTo(right);
    assertThat(right).isEqualTo(left);
    assertThat(left).hasSameHashCodeAs(right);

    Set<ConfigurationParameter> parameters = new HashSet<>();
    parameters.add(left);
    parameters.add(right);
    assertThat(parameters).hasSize(1);
  }

  @Test
  @DisplayName("parameters with different ids stay distinct")
  void differentIdsAreNotEqual() {
    ConfigurationParameter left = ConfigurationParameter.builder().id(7).name("a").build();
    ConfigurationParameter right = ConfigurationParameter.builder().id(8).name("a").build();

    assertThat(left).isNotEqualTo(right);
    assertThat(left).doesNotHaveSameHashCodeAs(right);

    Set<ConfigurationParameter> parameters = new HashSet<>();
    parameters.add(left);
    parameters.add(right);
    assertThat(parameters).hasSize(2);
  }

  @Test
  @DisplayName("a configuration parameter is not an application parameter with the same id")
  void notEqualToApplicationParameter() {
    ConfigurationParameter configuration =
        ConfigurationParameter.builder().id(7).name("language.default").build();
    ApplicationParameter application =
        ApplicationParameter.builder().id(7).name("language.default").build();

    assertThat(configuration).isNotEqualTo(application);
    assertThat(application).isNotEqualTo(configuration);
  }

  @Test
  @DisplayName("equality is reflexive and rejects null")
  void reflexiveAndRejectsNull() {
    ConfigurationParameter parameter = ConfigurationParameter.builder().id(7).name("a").build();

    assertThat(parameter).isEqualTo(parameter);
    assertThat(parameter).isNotEqualTo(null);
  }
}
