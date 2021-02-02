package org.sitmun.plugin.core.web.exceptions;

/**
 * Utility class for errors.
 */
public class ApiError {

  private String error;
  private String message;

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
