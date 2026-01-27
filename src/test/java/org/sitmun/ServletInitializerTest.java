package org.sitmun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

/** Tests for {@link ServletInitializer}. */
@DisplayName("ServletInitializer tests")
class ServletInitializerTest {

  @Test
  @DisplayName("Should configure Spring application builder successfully")
  void testConfigure() {
    // Given
    ServletInitializer servletInitializer = new ServletInitializer();
    SpringApplicationBuilder builder = new SpringApplicationBuilder();

    // When
    SpringApplicationBuilder result = servletInitializer.configure(builder);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isInstanceOf(SpringApplicationBuilder.class);
  }

  @Test
  @DisplayName("Should extend SpringBootServletInitializer")
  void testInheritance() {
    // Given
    ServletInitializer servletInitializer = new ServletInitializer();

    // Then
    assertThat(servletInitializer)
        .isInstanceOf(org.springframework.boot.web.servlet.support.SpringBootServletInitializer.class);
  }

  @Test
  @DisplayName("Should be instantiable")
  void testInstantiation() {
    // When
    ServletInitializer servletInitializer = new ServletInitializer();

    // Then
    assertThat(servletInitializer).isNotNull();
  }
}
