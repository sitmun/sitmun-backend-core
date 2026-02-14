package org.sitmun.infrastructure.startup;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValue;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValueRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "sitmun.startup.code-lists",
    name = "check-on-startup",
    havingValue = "true",
    matchIfMissing = true)
public class DatabaseConnectionDriverCodeListSetup implements ApplicationRunner {

  private final StartupCodelistsProperties properties;
  private final CodeListValueRepository repository;

  public DatabaseConnectionDriverCodeListSetup(
      StartupCodelistsProperties properties, CodeListValueRepository repository) {
    this.properties = properties;
    this.repository = repository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("Started DatabaseConnectionDriverCodeListSetup");
    List<String> existingConfiguration = new ArrayList<>();
    List<CodeListValue> newValues = new ArrayList<>();
    repository
        .findAllByCodeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER)
        .forEach(item -> existingConfiguration.add(item.getValue()));
    log.info("Found {} existing database connection drivers", existingConfiguration.size());

    properties
        .getDatabaseConnectionDrivers()
        .forEach(
            driver -> {
              if (!existingConfiguration.contains(driver.getDriverClass())) {
                CodeListValue codeListValue = new CodeListValue();
                codeListValue.setCodeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER);
                codeListValue.setValue(driver.getDriverClass());
                codeListValue.setDescription(driver.getDescription());
                codeListValue.setDefaultCode(false);
                codeListValue.setSystem(true);
                newValues.add(codeListValue);
                log.info("Will add database connection driver: {}", driver.getDriverClass());
              } else {
                log.debug(
                    "Skipping existing database connection driver: {}", driver.getDriverClass());
              }
            });

    if (!newValues.isEmpty()) {
      log.info("Saving {} new database connection drivers", newValues.size());
      try {
        repository.saveAll(newValues);
        log.info(
            "Finished DatabaseConnectionDriverCodeListSetup: successfully added {} new drivers",
            newValues.size());
      } catch (DataIntegrityViolationException e) {
        log.warn(
            "Some database connection drivers already exist in database, attempting individual insert");
        int successCount = 0;
        for (CodeListValue value : newValues) {
          try {
            repository.save(value);
            successCount++;
            log.debug("Successfully saved database connection driver: {}", value.getValue());
          } catch (DataIntegrityViolationException ex) {
            log.debug("Skipped duplicate database connection driver: {}", value.getValue());
          }
        }
        log.info(
            "Finished DatabaseConnectionDriverCodeListSetup: added {} out of {} drivers",
            successCount,
            newValues.size());
      }
    } else {
      log.info("Finished DatabaseConnectionDriverCodeListSetup: no new drivers to add");
    }
  }
}
