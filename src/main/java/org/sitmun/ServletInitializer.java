package org.sitmun;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Servlet initializer for WAR deployment.
 *
 * <p>This class enables the application to be deployed as a WAR file to external servlet containers
 * such as Tomcat, WildFly, or WebSphere. It is automatically included when building with {@code
 * -Ppackaging=war}.
 *
 * <p>For JAR deployment (default), this class is present but not used, as Spring Boot's embedded
 * Tomcat handles initialization.
 *
 * @see Application
 */
public class ServletInitializer extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
    return builder.sources(Application.class);
  }
}
