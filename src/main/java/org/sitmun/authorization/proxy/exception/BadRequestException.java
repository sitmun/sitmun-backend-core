package org.sitmun.authorization.proxy.exception;

public class BadRequestException extends IllegalArgumentException {

  public BadRequestException(String msg) {
    super(msg);
  }
}
