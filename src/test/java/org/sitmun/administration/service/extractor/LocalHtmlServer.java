package org.sitmun.administration.service.extractor;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class LocalHtmlServer implements AutoCloseable {

  private static final byte[] HOME =
      "<!DOCTYPE html><html><head></head><body>home</body></html>".getBytes(StandardCharsets.UTF_8);
  private static final byte[] MISSING = "<html><<not-xml".getBytes(StandardCharsets.UTF_8);

  private final HttpServer server;

  private LocalHtmlServer(HttpServer server) {
    this.server = server;
  }

  public static LocalHtmlServer start() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          boolean missing = "/not-found".equals(exchange.getRequestURI().getPath());
          byte[] body = missing ? MISSING : HOME;
          exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
          exchange.sendResponseHeaders(missing ? 404 : 200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    return new LocalHtmlServer(server);
  }

  public String url(String path) {
    return "http://127.0.0.1:" + server.getAddress().getPort() + path;
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
