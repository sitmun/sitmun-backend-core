package org.sitmun.administration.service.extractor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class HttpClientFactory {

  private final List<String> unsafeAllowedHosts;
  private final OkHttpClient safeClient;
  private final OkHttpClient unsafeClient;

  public HttpClientFactory(@Value("${sitmun.client.unsafe-allowed-hosts:*}") List<String> unsafeAllowedHosts) {
    this.unsafeAllowedHosts = unsafeAllowedHosts;
    safeClient = new OkHttpClient.Builder().build();
    unsafeClient = configureToIgnoreCertificate(new OkHttpClient.Builder()).build();
  }

  private static OkHttpClient.Builder configureToIgnoreCertificate(OkHttpClient.Builder builder) {
    log.warn("Ignore SSL Certificate");
    try {

      // Create a trust manager that does not validate certificate chains
      final TrustManager[] trustAllCerts = new TrustManager[]{
        new X509TrustManager() {
          @Override
          public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
          }

          @Override
          public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
          }

          @Override
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
          }
        }
      };

      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      // Create a ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
      builder.hostnameVerifier((hostname, session) -> true);
    } catch (Exception e) {
      log.warn("Exception while configuring IgnoreSslCertificate: {}", e.getMessage(), e);
    }
    return builder;
  }

  public Response executeRequest(Request httpRequest) throws IOException {
    return getClient(httpRequest.url().host()).newCall(httpRequest).execute();
  }

  private OkHttpClient getClient(String host) {
    try {
      if (unsafeAllowedHosts.contains("*") || unsafeAllowedHosts.contains(host)) {
        log.warn("Using Unsafe Client");
        return unsafeClient;
      } else {
        return safeClient;
      }
    } catch (Exception e) {
      log.warn("Exception while creating client: {}", e.getMessage(), e);
      return safeClient;
    }
  }
}
