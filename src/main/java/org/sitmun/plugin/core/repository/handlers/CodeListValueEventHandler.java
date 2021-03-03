package org.sitmun.plugin.core.repository.handlers;

import org.sitmun.plugin.core.domain.CodeListValue;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Component
@RepositoryEventHandler
public class CodeListValueEventHandler {

  @HandleBeforeCreate
  public void handleCodeListValueCreate(@NotNull CodeListValue codeListValue) {
    if (codeListValue.getSystem()) {
      throw new ImmutableSystemCodeListValueException("System field cannot be set true");
    }
    codeListValue.setSystem(false);
  }

  @HandleBeforeSave
  public void handleCodeListValueUpdate(@NotNull CodeListValue codeListValue) {
    if (codeListValue.getStoredSystem() != codeListValue.getSystem()) {
      throw new ImmutableSystemCodeListValueException("System field cannot be changed");
    }
    if (codeListValue.getStoredSystem()) {
      if (!Objects.equals(codeListValue.getStoredCodeListName(), codeListValue.getCodeListName())) {
        throw new ImmutableSystemCodeListValueException("Code list name cannot be changed in system property");
      }
      if (!Objects.equals(codeListValue.getStoredValue(), codeListValue.getValue())) {
        throw new ImmutableSystemCodeListValueException("Value cannot be changed in system property");
      }
    }
    codeListValue.setSystem(codeListValue.getStoredSystem());
  }

  @HandleBeforeDelete
  public void handleCodeListValueDelete(@NotNull CodeListValue codeListValue) {
    if (codeListValue.getSystem()) {
      throw new ImmutableSystemCodeListValueException("System field cannot be deleted");
    }
  }

}