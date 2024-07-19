package org.sitmun.infrastructure.persistence.liquibase;

import org.springframework.core.env.Profiles;

public class Constants {

  static final Profiles heroku = Profiles.of("heroku");

}
