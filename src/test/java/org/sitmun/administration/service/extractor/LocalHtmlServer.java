package org.sitmun.administration.service.extractor;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class LocalHtmlServer implements AutoCloseable {

  private static final byte[] HOME =
      "<!DOCTYPE html><html><head></head><body>home</body></html>".getBytes(StandardCharsets.UTF_8);
  private static final byte[] MISSING = "<html><<not-xml".getBytes(StandardCharsets.UTF_8);
  private static final byte[] WFS_EXCEPTION =
      ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
              + "<ows:ExceptionReport xmlns:ows=\"http://www.opengis.net/ows/1.1\" version=\"2.0.0\">"
              + "<ows:Exception exceptionCode=\"MissingParameterValue\" locator=\"request\"/>"
              + "</ows:ExceptionReport>")
          .getBytes(StandardCharsets.UTF_8);

  private final HttpServer server;

  private LocalHtmlServer(HttpServer server) {
    this.server = server;
  }

  public static LocalHtmlServer start() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String path = exchange.getRequestURI().getPath();
          byte[] body = HOME;
          int status = 200;
          String contentType = "text/html; charset=UTF-8";
          if ("/not-found".equals(path)) {
            body = MISSING;
            status = 404;
          } else if ("/wfs-exception".equals(path)) {
            body = WFS_EXCEPTION;
            status = 400;
            contentType = "application/xml; charset=UTF-8";
          }
          exchange.getResponseHeaders().set("Content-Type", contentType);
          exchange.sendResponseHeaders(status, body.length);
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
