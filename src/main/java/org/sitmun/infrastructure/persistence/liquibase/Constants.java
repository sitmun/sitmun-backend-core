package org.sitmun.infrastructure.persistence.liquibase;

import org.springframework.core.env.Profiles;

public class Constants {

  private Constants() {
    // Prevent instantiation
  }

  static final Profiles heroku = Profiles.of("heroku");
}
