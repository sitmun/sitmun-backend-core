package org.sitmun.infrastructure.security.password.exception;

/**
 * Exception thrown when mail service is not available. This exception is thrown when mail features
 * are requested but the mail profile is not active.
 */
public class MailNotImplementedException extends RuntimeException {

  public MailNotImplementedException(String message) {
    super(message);
  }
}
