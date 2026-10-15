package org.sitmun.administration.service.extractor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.List;
import javax.net.ssl.SSLHandshakeException;
import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HttpClientFactory tests")
class HttpClientFactoryTest {

  private static LocalTlsServer tls;

  @BeforeAll
  static void startTls() throws Exception {
    tls = LocalTlsServer.start();
  }

  @AfterAll
  static void stopTls() throws IOException {
    tls.close();
  }

  @Test
  @DisplayName("Fail with SSLHandshakeException")
  void failWithASSLHandhakeException() {
    HttpClientFactory client = new HttpClientFactory(Lists.list());

    Request request = new Request.Builder().url(tls.url()).header("Accept", "*/*").build();

    assertThrows(
        SSLHandshakeException.class,
        () -> {
          //noinspection EmptyTryBlock
          try (Response ignored = client.executeRequest(request)) {}
        });
  }

  @Test
  @DisplayName("Any request use the unsafe client")
  void anyRequestUseTheUnsafeClient() {
    assertCompletes(Lists.list("*"));
  }

  @Test
  @DisplayName("Use unsafe client when domain matches")
  void useUnsafeClientWhenDomainMatches() {
    assertCompletes(Lists.list(tls.host()));
  }

  private static void assertCompletes(List<String> unsafeAllowedHosts) {
    HttpClientFactory client = new HttpClientFactory(unsafeAllowedHosts);
    Request request = new Request.Builder().url(tls.url()).header("Accept", "*/*").build();

    try (Response response = client.executeRequest(request)) {
      assertTrue(response.isSuccessful());
    } catch (IOException e) {
      fail(e);
    }
  }
}
