package org.sitmun.resetPassword.exception;

/** Exception thrown when there's an error rendering email templates. */
public class EmailTemplateException extends RuntimeException {
  public EmailTemplateException(String message, Throwable cause) {
    super(message, cause);
  }
}
