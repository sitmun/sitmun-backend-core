package org.sitmun.infrastructure.persistence.type.i18n;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

@Component
@RepositoryEventHandler
public class LanguageEventHandler {

  @HandleBeforeSave
  public void handleLanguageUpdate(@NotNull Language language) {
    if (language.getStoredShortname() != null
        && !Objects.equals(language.getStoredShortname(), language.getShortname())) {
      throw new IllegalStateException("Language code cannot be changed after creation");
    }
  }
}
