package org.sitmun.authorization.proxy.exception;

import org.springframework.security.core.AuthenticationException;

public class BadRequestException extends AuthenticationException {

  public BadRequestException(String msg) {
    super(msg);
  }
}
