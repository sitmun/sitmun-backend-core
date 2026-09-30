package org.sitmun.administration.service.extractor;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

public final class LocalTlsServer implements AutoCloseable {

  private static final byte[] BODY = "ok".getBytes(StandardCharsets.UTF_8);
  private static final char[] PASSWORD = "changeit".toCharArray();

  private final HttpsServer server;
  private final Path keystoreDir;

  private LocalTlsServer(HttpsServer server, Path keystoreDir) {
    this.server = server;
    this.keystoreDir = keystoreDir;
  }

  public static LocalTlsServer start() throws Exception {
    Path keystoreDir = Files.createTempDirectory("sitmun-tls");
    Path keystore = keystoreDir.resolve("keystore.p12");
    Path keytool = Path.of(System.getProperty("java.home"), "bin", "keytool");
    Process process =
        new ProcessBuilder(
                keytool.toString(),
                "-genkeypair",
                "-alias",
                "sitmun",
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-storetype",
                "PKCS12",
                "-keystore",
                keystore.toString(),
                "-storepass",
                new String(PASSWORD),
                "-keypass",
                new String(PASSWORD),
                "-dname",
                "CN=127.0.0.1",
                "-validity",
                "1",
                "-noprompt")
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.waitFor() != 0) {
      throw new IllegalStateException(output);
    }

    KeyStore store = KeyStore.getInstance("PKCS12");
    try (InputStream in = Files.newInputStream(keystore)) {
      store.load(in, PASSWORD);
    }
    KeyManagerFactory keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keys.init(store, PASSWORD);
    SSLContext context = SSLContext.getInstance("TLS");
    context.init(keys.getKeyManagers(), null, null);

    HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setHttpsConfigurator(new HttpsConfigurator(context));
    server.createContext(
        "/",
        exchange -> {
          exchange.sendResponseHeaders(200, BODY.length);
          exchange.getResponseBody().write(BODY);
          exchange.close();
        });
    server.start();
    return new LocalTlsServer(server, keystoreDir);
  }

  public String url() {
    return "https://127.0.0.1:" + server.getAddress().getPort() + "/";
  }

  public String host() {
    return "127.0.0.1";
  }

  @Override
  public void close() throws IOException {
    server.stop(0);
    try (var files = Files.list(keystoreDir)) {
      for (Path file : files.toList()) {
        Files.deleteIfExists(file);
      }
    }
    Files.deleteIfExists(keystoreDir);
  }
}
