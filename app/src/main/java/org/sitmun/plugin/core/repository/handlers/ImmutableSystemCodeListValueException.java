package org.sitmun.plugin.core.repository.handlers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ImmutableSystemCodeListValueException extends RuntimeException {
  public ImmutableSystemCodeListValueException(String msg) {
    super(msg);
  }
}
