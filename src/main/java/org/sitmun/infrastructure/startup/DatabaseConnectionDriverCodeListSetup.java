package org.sitmun.infrastructure.startup;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.CodeListsConstants;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValue;
import org.sitmun.infrastructure.persistence.type.codelist.CodeListValueRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "sitmun.startup.code-lists", name = "check-on-startup", havingValue = "true", matchIfMissing = true)
public class DatabaseConnectionSetup implements ApplicationRunner {

  private StartupCodelistsProperties properties;
  private CodeListValueRepository repository;

  public DatabaseConnectionSetup(StartupCodelistsProperties properties, CodeListValueRepository repository) {
    this.properties = properties;
    this.repository = repository;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("Checking codelist {}", CodeListsConstants.DATABASE_CONNECTION_DRIVER);
    List<String> existingConfiguration = new ArrayList<>();
    repository.findAllByCodeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER).forEach(item -> existingConfiguration.add(item.getStoredValue()));
    properties.getDatabaseConnectionDrivers().forEach(driver -> {
      if (!existingConfiguration.contains(driver.getDriverClass())) {
        CodeListValue codeListValue = new CodeListValue();
        codeListValue.setCodeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER);
        codeListValue.setValue(driver.getDriverClass());
        codeListValue.setDescription(driver.getDescription());
        codeListValue.setDefaultCode(false);
        codeListValue.setSystem(true);
        repository.save(codeListValue);
        log.info("Added database connection driver: {}", driver.getDriverClass());
      }
    });
  }
}
