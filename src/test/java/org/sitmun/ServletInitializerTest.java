package org.sitmun;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests for {@link ServletInitializer}. */
@DisplayName("ServletInitializer tests")
class ServletInitializerTest {

  @Test
  @DisplayName("Should extend SpringBootServletInitializer")
  void testInheritance() {
    // Given
    ServletInitializer servletInitializer = new ServletInitializer();

    // Then
    assertThat(servletInitializer)
        .isInstanceOf(
            org.springframework.boot.web.servlet.support.SpringBootServletInitializer.class);
  }
}
