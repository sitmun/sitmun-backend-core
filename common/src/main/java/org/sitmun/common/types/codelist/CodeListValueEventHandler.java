package org.sitmun.common.types.codelist;

import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.HandleBeforeDelete;
import org.springframework.data.rest.core.annotation.HandleBeforeSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Component
@RepositoryEventHandler
public class CodeListValueEventHandler {

  @HandleBeforeCreate
  @Transactional(rollbackFor = ImmutableSystemCodeListValueException.class)
  public void handleCodeListValueCreate(@NotNull CodeListValue codeListValue) {
    if (codeListValue.getSystem()) {
      throw new ImmutableSystemCodeListValueException("System field cannot be set true");
    }
    codeListValue.setSystem(false);
  }

  @HandleBeforeSave
  @Transactional(rollbackFor = ImmutableSystemCodeListValueException.class)
  public void handleCodeListValueUpdate(@NotNull CodeListValue codeListValue) {
    if (!Objects.equals(codeListValue.getStoredSystem(), codeListValue.getSystem())) {
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
  @Transactional(rollbackFor = ImmutableSystemCodeListValueException.class)
  public void handleCodeListValueDelete(@NotNull CodeListValue codeListValue) {
    if (codeListValue.getSystem()) {
      throw new ImmutableSystemCodeListValueException("System field cannot be deleted");
    }
  }

}