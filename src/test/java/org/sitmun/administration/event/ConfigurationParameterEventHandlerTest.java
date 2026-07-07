package org.sitmun.administration.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.domain.configuration.ConfigurationParameter;

@DisplayName("ConfigurationParameter Protection Tests")
class ConfigurationParameterEventHandlerTest {

  private final ConfigurationParameterEventHandler handler =
      new ConfigurationParameterEventHandler();

  @Test
  @DisplayName("Prevent creation of language.default parameter")
  void preventCreationOfLanguageDefault() {
    ConfigurationParameter param =
        ConfigurationParameter.builder().name("language.default").value("fr").build();

    assertThatThrownBy(() -> handler.handleBeforeCreate(param))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("language.default")
        .hasMessageContaining("language default change API");
  }

  @Test
  @DisplayName("Prevent update of language.default parameter")
  void preventUpdateOfLanguageDefault() {
    ConfigurationParameter param =
        ConfigurationParameter.builder().id(1).name("language.default").value("fr").build();

    assertThatThrownBy(() -> handler.handleBeforeSave(param))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("language.default")
        .hasMessageContaining("language default change API");
  }

  @Test
  @DisplayName("Prevent deletion of language.default parameter")
  void preventDeletionOfLanguageDefault() {
    ConfigurationParameter param =
        ConfigurationParameter.builder().id(1).name("language.default").value("en").build();

    assertThatThrownBy(() -> handler.handleBeforeDelete(param))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("language.default");
  }

  @Test
  @DisplayName("Allow modification of other configuration parameters")
  void allowModificationOfOtherParameters() {
    ConfigurationParameter param =
        ConfigurationParameter.builder().id(1).name("test.parameter").value("test-value").build();

    // Should not throw any exception
    handler.handleBeforeCreate(param);
    handler.handleBeforeSave(param);
    handler.handleBeforeDelete(param);

    assertThat(true).isTrue(); // If we reach here, no exception was thrown
  }
}
