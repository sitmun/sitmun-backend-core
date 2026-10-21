package org.sitmun.administration.config;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.domain.application.ApplicationPointOfContactValidation;
import org.sitmun.domain.application.BeforeSaveApplicationValidator;
import org.sitmun.domain.background.BeforeCreateBackgroundValidator;
import org.sitmun.domain.cartography.permission.BeforeSaveCartographyPermissionValidator;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.validation.Validator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Administration REST validator registration")
class AdministrationRestConfigurerTest {

  @Mock private EntityManager entityManager;
  @Mock private ListableBeanFactory beanFactory;
  @Mock private ValidatingRepositoryEventListener listener;
  @Mock private ApplicationPointOfContactValidation pocValidation;

  private AdministrationRestConfigurer configurer;
  private BeforeSaveApplicationValidator applicationValidator;
  private BeforeCreateBackgroundValidator backgroundValidator;
  private BeforeSaveCartographyPermissionValidator permissionValidator;

  @BeforeEach
  void setUp() {
    applicationValidator = new BeforeSaveApplicationValidator(pocValidation);
    backgroundValidator = new BeforeCreateBackgroundValidator();
    permissionValidator = new BeforeSaveCartographyPermissionValidator();
    when(beanFactory.getBeansOfType(Validator.class))
        .thenReturn(
            Map.of(
                "beforeSaveApplicationValidator",
                applicationValidator,
                "beforeCreateBackgroundValidator",
                backgroundValidator,
                "beforeSaveCartographyPermissionValidator",
                permissionValidator));
    configurer = new AdministrationRestConfigurer(entityManager, beanFactory);
  }

  @Test
  @DisplayName("application save rule is registered once on beforeSave and beforeLinkSave")
  void applicationSaveRuleRunsOnSaveAndLinkSave() {
    configurer.configureValidatingRepositoryEventListener(listener);

    verify(listener, times(1)).addValidator("beforeSave", applicationValidator);
    verify(listener, times(1)).addValidator("beforeLinkSave", applicationValidator);
    verify(listener, never()).addValidator("beforeCreate", applicationValidator);
  }

  @Test
  @DisplayName("background map rule is registered once on beforeCreate and beforeLinkSave")
  void backgroundMapRuleRunsOnCreateAndLinkSave() {
    configurer.configureValidatingRepositoryEventListener(listener);

    verify(listener, times(1)).addValidator("beforeCreate", backgroundValidator);
    verify(listener, times(1)).addValidator("beforeLinkSave", backgroundValidator);
    verify(listener, never()).addValidator("beforeSave", backgroundValidator);
  }

  @Test
  @DisplayName("a single-event validator stays on the event named by its bean")
  void prefixOnlyValidatorStaysOnOneEvent() {
    configurer.configureValidatingRepositoryEventListener(listener);

    verify(listener, times(1)).addValidator("beforeSave", permissionValidator);
    verify(listener, never()).addValidator("beforeLinkSave", permissionValidator);
    verify(listener, never()).addValidator("beforeCreate", permissionValidator);
  }
}
