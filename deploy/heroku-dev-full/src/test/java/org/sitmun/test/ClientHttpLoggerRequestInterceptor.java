package org.sitmun.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ClientHttpLoggerRequestInterceptor implements ClientHttpRequestInterceptor {

  static final Logger LOGGER = LoggerFactory.getLogger(ClientHttpLoggerRequestInterceptor.class);

  @SuppressWarnings("NullableProblems")
  @Override
  public ClientHttpResponse intercept(
    HttpRequest req, byte[] reqBody, ClientHttpRequestExecution ex) throws IOException {
    LOGGER.debug("Request body:\n{}", new String(reqBody, StandardCharsets.UTF_8));
    ClientHttpResponse response = ex.execute(req, reqBody);
    try {
      InputStreamReader isr = new InputStreamReader(
        response.getBody(), StandardCharsets.UTF_8);
      String body = new BufferedReader(isr).lines()
        .collect(Collectors.joining("\n"));
      LOGGER.debug("Response body:\n{}", body);
    } catch (IOException e) {
      LOGGER.debug("No response body");
    }
    return response;
  }
}