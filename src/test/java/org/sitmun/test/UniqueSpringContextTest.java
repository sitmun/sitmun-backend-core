package org.sitmun.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.sitmun.Application;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.BootstrapUtils;
import org.springframework.test.context.MergedContextConfiguration;

class UniqueSpringContextTest {

  private static final String USER_POSITION_REPOSITORY =
      "org.sitmun.domain.user.position.UserPositionRepositoryDataRestTest";
  private static final String USER_REPOSITORY = "org.sitmun.domain.user.UserRepositoryDataRestTest";
  private static final String SPATIAL_REFERENCE =
      "org.sitmun.infrastructure.persistence.type.srs.SpatialReferenceSystemTest";
  private static final String EMAIL = "org.sitmun.infrastructure.persistence.type.basic.EmailTest";
  private static final String USER_POSITION_LOGIC =
      "org.sitmun.domain.user.position.UserPositionBusinessLogicTest";
  private static final String USER_CHECKS = "org.sitmun.domain.user.UserChecksServiceTest";
  private static final String JWT =
      "org.sitmun.infrastructure.security.service.JsonWebTokenServiceTest";
  private static final String RESET_PASSWORD =
      "org.sitmun.infrastructure.security.password.controller.ResetPasswordControllerTest";
  private static final String TEMPLATE_SECURITY =
      "org.sitmun.infrastructure.security.config.TemplateMoreInfoAdvancedRenderSecurityTest";
  private static final String LANGUAGE =
      "org.sitmun.administration.controller.DefaultLanguageChangeControllerTest";
  private static final String TASK_LITERAL =
      "org.sitmun.domain.task.TaskLiteralEnrollmentDataRestTest";
  private static final String DATABASE_CONNECTION =
      "org.sitmun.administration.controller.DatabaseConnectionControllerTest";
  private static final String CODE_LIST =
      "org.sitmun.infrastructure.persistence.type.codelist.CodeListTest";

  @Test
  void userPositionRepositorySharesTheDataRestContext() {
    assertThat(key(USER_POSITION_REPOSITORY)).isEqualTo(key(USER_REPOSITORY));
  }

  @Test
  void spatialReferenceSystemSharesTheBaseContext() {
    assertThat(key(SPATIAL_REFERENCE)).isEqualTo(key(EMAIL));
  }

  @Test
  void userPositionBusinessLogicSharesTheBaseContext() {
    assertThat(key(USER_POSITION_LOGIC)).isEqualTo(key(EMAIL));
  }

  @Test
  void userChecksServiceDoesNotBootAContext() {
    Class<?> testClass = type(USER_CHECKS);
    assertThat(testClass.getAnnotation(SpringBootTest.class)).isNull();
    assertThat(testClass.getAnnotation(DataJpaTest.class)).isNull();
  }

  @Test
  void jsonWebTokenServiceDoesNotBootTheApplication() {
    assertThat(key(JWT).getClasses()).doesNotContain(Application.class);
    assertThat(reason(JWT)).contains("clock");
  }

  @Test
  void listedUniqueContextsNameTheBehavior() {
    assertThat(reason(RESET_PASSWORD)).containsIgnoringCase("mail");
    assertThat(reason(TEMPLATE_SECURITY)).containsIgnoringCase("template");
    assertThat(reason(LANGUAGE)).containsIgnoringCase("language");
    assertThat(reason(TASK_LITERAL)).containsIgnoringCase("spy");
    assertThat(reason(DATABASE_CONNECTION)).containsIgnoringCase("driver");
  }

  @Test
  void codeListStaysOnBaseTest() {
    Class<?> testClass = type(CODE_LIST);
    assertThat(testClass.getSuperclass()).isEqualTo(BaseTest.class);
    assertThat(testClass.getAnnotation(UniqueContext.class)).isNull();
  }

  private static MergedContextConfiguration key(String className) {
    return BootstrapUtils.resolveTestContextBootstrapper(type(className))
        .buildMergedContextConfiguration();
  }

  private static String reason(String className) {
    Class<?> testClass = type(className);
    UniqueContext marker = testClass.getAnnotation(UniqueContext.class);
    assertThat(marker).as(testClass.getSimpleName()).isNotNull();
    return marker.value();
  }

  private static Class<?> type(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
