package org.sitmun.domain.cartography.style;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.cartography.Cartography;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cartography style default validators")
class CartographyStyleDefaultValidatorTest {

  private static final String ALREADY_DEFAULT =
      "Already a default style exists for the cartography.";

  @Mock private CartographyStyleRepository repository;

  private Cartography cartography;
  private CartographyStyle style;
  private BeforeCreateCartographyStylesValidator createValidator;
  private BeforeSaveCartographyStylesValidator saveValidator;

  @BeforeEach
  void setUp() {
    cartography = Cartography.builder().id(1).build();
    style =
        CartographyStyle.builder()
            .id(10)
            .name("test")
            .defaultStyle(true)
            .cartography(cartography)
            .build();
    createValidator = new BeforeCreateCartographyStylesValidator(repository);
    saveValidator = new BeforeSaveCartographyStylesValidator(repository);
  }

  @Test
  @DisplayName("create accepts the first default style")
  void createAcceptsTheFirstDefault() {
    when(repository.countDefaultStyles(cartography)).thenReturn(0);

    Errors errors = errors();
    createValidator.validate(style, errors);

    assertThat(errors.hasErrors()).isFalse();
    verify(repository, never()).countDefaultStylesButThis(cartography, style);
  }

  @Test
  @DisplayName("create rejects a second default on defaultStyle")
  void createRejectsASecondDefaultOnDefaultStyle() {
    when(repository.countDefaultStyles(cartography)).thenReturn(1);

    Errors errors = errors();
    createValidator.validate(style, errors);

    assertThat(errors.getFieldError("defaultStyle")).isNotNull();
    assertThat(errors.getFieldError("defaultStyle").getDefaultMessage()).isEqualTo(ALREADY_DEFAULT);
    verify(repository, never()).countDefaultStylesButThis(cartography, style);
  }

  @Test
  @DisplayName("save accepts the style that is already the default")
  void saveAcceptsTheExistingDefault() {
    when(repository.countDefaultStylesButThis(cartography, style)).thenReturn(0);

    Errors errors = errors();
    saveValidator.validate(style, errors);

    assertThat(errors.hasErrors()).isFalse();
    verify(repository, never()).countDefaultStyles(cartography);
  }

  @Test
  @DisplayName("save rejects another default on defaultStyle")
  void saveRejectsAnotherDefaultOnDefaultStyle() {
    when(repository.countDefaultStylesButThis(cartography, style)).thenReturn(1);

    Errors errors = errors();
    saveValidator.validate(style, errors);

    assertThat(errors.getFieldError("defaultStyle")).isNotNull();
    assertThat(errors.getFieldError("defaultStyle").getDefaultMessage()).isEqualTo(ALREADY_DEFAULT);
    assertThat(errors.getFieldError("styles")).isNull();
    verify(repository, never()).countDefaultStyles(cartography);
  }

  @Test
  @DisplayName("create and save both accept a non-default style")
  void nonDefaultStyleIsAcceptedOnCreateAndSave() {
    style.setDefaultStyle(false);

    Errors createErrors = errors();
    createValidator.validate(style, createErrors);
    Errors saveErrors = errors();
    saveValidator.validate(style, saveErrors);

    assertThat(createErrors.hasErrors()).isFalse();
    assertThat(saveErrors.hasErrors()).isFalse();
    verify(repository, never()).countDefaultStyles(cartography);
    verify(repository, never()).countDefaultStylesButThis(cartography, style);
  }

  private Errors errors() {
    return new BeanPropertyBindingResult(style, "cartographyStyle");
  }
}
