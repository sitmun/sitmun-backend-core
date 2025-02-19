package org.sitmun.infrastructure.startup;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValue;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValueRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DatabaseConnectionSetup implements ApplicationRunner {

  private CodeListValueRepository repository;

  public DatabaseConnectionSetup(CodeListValueRepository repository) {
    this.repository = repository;
  }


  private static final Map<String, CodeListValue> supportedDatabaseDrivers = new HashMap<>(Map.of(
    "org.postgresql.Driver",
    CodeListValue.builder().
      codeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER).
      value("org.postgresql.Driver").
      system(true).
      defaultCode(false).
      description("PostgreSQL JDBC driver").
      build(),
    "oracle.jdbc.OracleDriver",
    CodeListValue.builder().
      codeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER).
      value("oracle.jdbc.OracleDriver").
      system(true).
      defaultCode(false).
      description("Oracle JDBC driver").
      build(),
    "org.h2.Driver",
    CodeListValue.builder().
      codeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER).
      value("org.h2.Driver").
      system(true).
      defaultCode(false).
      description("H2 JDBC driver").
      build()
  ));

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    repository.findAllByCodeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER)
      .forEach(item -> supportedDatabaseDrivers.remove(item.getStoredValue()));
    repository.saveAll(supportedDatabaseDrivers.values());
    supportedDatabaseDrivers.values().forEach(item -> log.info("Registered: {}", item.getDescription()));
  }
}
