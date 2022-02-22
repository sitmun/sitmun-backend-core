package org.sitmun.annotation;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootApplication
@ComponentScan("org.sitmun.plugin.core")
@EntityScan(basePackages = "org.sitmun.domain")
@EnableJpaRepositories("org.sitmun.repository")
public @interface SitmunApplication {
}
