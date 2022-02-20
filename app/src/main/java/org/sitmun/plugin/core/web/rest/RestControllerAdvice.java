package org.sitmun.plugin.core.web.rest;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@ControllerAdvice
public class RestControllerAdvice {

  @ExceptionHandler(AuthenticationException.class)
  public void handleAccessDeniedException(HttpServletResponse response) throws IOException {
    response.sendError(HttpStatus.UNAUTHORIZED.value());
  }

}
