package org.sitmun.plugin.core.web.exceptions;

/**
 * Utility class for errors.
 */
public class ApiError {

  private String error;
  private String cause;

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getCause() {
    return cause;
  }

  public void setCause(String cause) {
    this.cause = cause;
  }
}
