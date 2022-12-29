package org.sitmun.common.domain.database;

import org.sitmun.common.def.CodeListsConstants;
import org.sitmun.common.domain.OnStartupUpdateHandler;
import org.sitmun.common.types.codelist.CodeListValue;
import org.sitmun.common.types.codelist.CodeListValueRepository;
import org.springframework.data.util.Streamable;
import org.springframework.stereotype.Component;

import java.sql.Driver;
import java.util.*;

@Component
public class DatabaseConnectionDriverUpdateHandler implements OnStartupUpdateHandler {

  private final CodeListValueRepository codeListValueRepository;

  public DatabaseConnectionDriverUpdateHandler(CodeListValueRepository codeListValueRepository) {
    this.codeListValueRepository = codeListValueRepository;
  }

  @Override
  public void update() {
    List<CodeListValue> toRemove = new ArrayList<>(Streamable.of(codeListValueRepository.findAllByCodeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER)).toList());
    ServiceLoader<Driver> loadedDrivers = ServiceLoader.load(Driver.class);

    List<CodeListValue> toAdd = new ArrayList<>();
    loadedDrivers.iterator().forEachRemaining(it -> {
      String driver = it.getClass().getCanonicalName();
      Optional<CodeListValue> exists = toRemove.stream().filter(r -> Objects.equals(r.getValue(), driver)).findFirst();
      if (exists.isPresent()) {
        toRemove.remove(exists.get());
      } else {
        CodeListValue newCodeListValue = CodeListValue.builder()
          .codeListName(CodeListsConstants.DATABASE_CONNECTION_DRIVER)
          .value(driver)
          .system(true)
          .description(driver)
          .defaultCode(false)
          .build();
        toAdd.add(newCodeListValue);
      }
    });
    codeListValueRepository.deleteAll(toRemove);
    codeListValueRepository.saveAll(toAdd);
  }
}
