package org.sitmun.administration.service.extractor;

import okhttp3.Request;
import okhttp3.Response;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("HttpClientFactory tests")
class HttpClientFactoryTest {

  @Test
  @DisplayName("Fail with SSLHandshakeException")
  void failWithASSLHandhakeException() {
    String url = "https://untrusted-root.badssl.com/";
    List<String> unsafeAllowedHosts = Lists.list();
    HttpClientFactory client = new HttpClientFactory(unsafeAllowedHosts);

    Request request = new Request.Builder()
      .url(url)
      .header("Accept", "*/*")
      .build();

    assertThrows(SSLHandshakeException.class, () -> {
      //noinspection EmptyTryBlock
      try (Response ignored = client.executeRequest(request)) {
        // Do nothing
      }
    });
  }

  @Test
  @DisplayName("Any request use the unsafe client")
  void anyRequestUseTheUnsafeClient() {
    String url = "https://ovc.catastro.meh.es/Cartografia/WMS/ServidorWMS.aspx";
    List<String> unsafeAllowedHosts = Lists.list("*");
    HttpClientFactory client = new HttpClientFactory(unsafeAllowedHosts);

    Request request = new Request.Builder()
      .url(url)
      .header("Accept", "*/*")
      .build();

    //noinspection EmptyTryBlock
    try(Response ignored = client.executeRequest(request)) {
      // Do nothing
    } catch (IOException e) {
      fail(e);
    }
  }

  @Test
  @DisplayName("Use unsafe client when domain matches")
  void useUnsafeClientWhenDomainMatches() {
    String url = "https://ovc.catastro.meh.es/Cartografia/WMS/ServidorWMS.aspx";
    List<String> unsafeAllowedHosts = Lists.list("ovc.catastro.meh.es");
    HttpClientFactory client = new HttpClientFactory(unsafeAllowedHosts);

    Request request = new Request.Builder()
      .url(url)
      .header("Accept", "*/*")
      .build();

    //noinspection EmptyTryBlock
    try(Response ignored = client.executeRequest(request)) {
      // Do nothing
    } catch (IOException e) {
      fail(e);
    }
  }
}