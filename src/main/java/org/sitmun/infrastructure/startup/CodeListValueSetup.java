package org.sitmun.infrastructure.startup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
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
public class CodeListValueSetup implements ApplicationRunner {

  private final StartupCodelistsProperties properties;
  private final CodeListValueRepository repository;
  private final StartupReady startupReady;

  public CodeListValueSetup(
      StartupCodelistsProperties properties,
      CodeListValueRepository repository,
      StartupReady startupReady) {
    this.properties = properties;
    this.repository = repository;
    this.startupReady = startupReady;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("Started CodeListValueSetup");
    Map<String, Map<String, CodeListValue>> existingConfiguration = new HashMap<>();
    repository
        .findAll()
        .forEach(
            item -> {
              if (!existingConfiguration.containsKey(item.getCodeListName())) {
                existingConfiguration.put(item.getCodeListName(), new HashMap<>());
              }
              existingConfiguration.get(item.getCodeListName()).put(item.getValue(), item);
            });
    log.info(
        "Found {} existing code list entries",
        existingConfiguration.values().stream().mapToInt(Map::size).sum());

    List<CodeListValue> newValues = new ArrayList<>();
    properties
        .getCodeListValues()
        .forEach(
            item -> {
              Map<String, CodeListValue> currentList =
                  existingConfiguration.get(item.getCodeListName());
              if (currentList == null || !currentList.containsKey(item.getValue())) {
                CodeListValue codeListValue = new CodeListValue();
                codeListValue.setCodeListName(item.getCodeListName());
                codeListValue.setValue(item.getValue());
                codeListValue.setDescription(item.getDescription());
                codeListValue.setDefaultCode(item.isDefault());
                codeListValue.setSystem(true);
                newValues.add(codeListValue);
                log.info("Will add code list value {}:{}", item.getCodeListName(), item.getValue());
              } else {
                log.debug(
                    "Skipping existing code list value {}:{}",
                    item.getCodeListName(),
                    item.getValue());
              }
            });

    if (!newValues.isEmpty()) {
      log.info("Saving {} new code list values", newValues.size());
      try {
        repository.saveAll(newValues);
        log.info("Finished CodeListValueSetup: successfully added {} new values", newValues.size());
        startupReady.setReady(true);
      } catch (DataIntegrityViolationException e) {
        log.warn("Some code list values already exist in database, attempting individual insert");
        int successCount = 0;
        for (CodeListValue value : newValues) {
          try {
            repository.save(value);
            successCount++;
            log.debug(
                "Successfully saved code list value {}:{}",
                value.getCodeListName(),
                value.getValue());
          } catch (DataIntegrityViolationException ex) {
            log.debug(
                "Skipped duplicate code list value {}:{}",
                value.getCodeListName(),
                value.getValue());
          }
        }
        log.info(
            "Finished CodeListValueSetup: added {} out of {} values",
            successCount,
            newValues.size());
        startupReady.setReady(true);
      }
    } else {
      log.info("Finished CodeListValueSetup: no new values to add");
      startupReady.setReady(true);
    }
  }
}
