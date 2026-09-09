package org.sitmun.administration.service.template.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.sitmun.domain.DomainConstants;

/** Loads PDF resources with bounded, public-network-only access. */
final class PdfExternalResourceLoader {
  private static final int REQUEST_TIMEOUT_MILLIS = 5_000;
  private static final int MAX_RESOURCES = 32;
  private static final int MAX_BYTES = 16_000_000;
  private static final int MAX_DURATION_SECONDS = 15;
  private static final int MAX_REDIRECTS = 5;
  private static final byte[] EMPTY = new byte[0];

  private final Dns dns;
  private final OkHttpClient httpClient;

  PdfExternalResourceLoader() {
    this(
        Dns.SYSTEM,
        new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build());
  }

  PdfExternalResourceLoader(Dns dns, OkHttpClient httpClient) {
    this.dns = dns;
    this.httpClient = httpClient;
  }

  boolean isAllowed(String uri) {
    if (uri == null) return false;
    try {
      String scheme = URI.create(uri).getScheme();
      return "data".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  Session newSession() {
    return new Session();
  }

  /** Keeps cache, byte budget and deadline shared by measurement and final rendering. */
  final class Session {
    private final Map<String, byte[]> cache = new HashMap<>();
    private final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(MAX_DURATION_SECONDS);
    private int totalBytes;

    byte[] fetch(String uri) {
      byte[] cached = cache.get(uri);
      if (cached != null) return cached;
      if (!isAllowed(uri) || cache.size() >= MAX_RESOURCES || remainingTimeoutMillis() <= 0) return EMPTY;
      byte[] content = fetchUncached(uri);
      if (content.length > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES
          || totalBytes + content.length > MAX_BYTES) content = EMPTY;
      totalBytes += content.length;
      cache.put(uri, content);
      return content;
    }

    private byte[] fetchUncached(String uri) {
      try {
        URI current = URI.create(uri);
        if ("data".equalsIgnoreCase(current.getScheme())) return decodeData(current.toString());
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
          int timeout = remainingTimeoutMillis();
          List<InetAddress> addresses = publicAddresses(current);
          if (timeout <= 0 || addresses.isEmpty()) return EMPTY;
          String host = normalizeHost(current.getHost());
          OkHttpClient client = httpClient.newBuilder().dns(requested -> pinned(requested, host, addresses))
              .proxy(Proxy.NO_PROXY).followRedirects(false).followSslRedirects(false)
              .connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS)
              .callTimeout(timeout, TimeUnit.MILLISECONDS).build();
          try (Response response = client.newCall(new Request.Builder().url(current.toString()).get().build()).execute()) {
            if (isRedirect(response.code())) {
              if (redirects == MAX_REDIRECTS || response.header("Location") == null) return EMPTY;
              current = current.resolve(response.header("Location"));
              continue;
            }
            if (!response.isSuccessful() || response.body() == null) return EMPTY;
            try (ResponseBody body = response.body(); var input = body.byteStream()) {
              return input.readNBytes(Math.min(DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES, MAX_BYTES - totalBytes) + 1);
            }
          }
        }
      } catch (IOException | RuntimeException e) {
        return EMPTY;
      }
      return EMPTY;
    }

    private byte[] decodeData(String uri) {
      int separator = uri.indexOf(',');
      if (separator < 0) return EMPTY;
      String encoded = uri.substring(separator + 1);
      boolean base64 = uri.substring(0, separator).toLowerCase(Locale.ROOT).endsWith(";base64");
      long max = base64 ? ((long) DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES + 2) / 3 * 4
          : (long) DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES * 3;
      if (encoded.length() > max) return EMPTY;
      try { return base64 ? Base64.getDecoder().decode(encoded) : decodePercent(encoded); }
      catch (IllegalArgumentException e) { return EMPTY; }
    }

    private byte[] decodePercent(String encoded) {
      ByteArrayOutputStream decoded = new ByteArrayOutputStream();
      for (int i = 0; i < encoded.length();) {
        char current = encoded.charAt(i);
        if (current == '%') {
          if (i + 2 >= encoded.length()) return EMPTY;
          int high = Character.digit(encoded.charAt(i + 1), 16), low = Character.digit(encoded.charAt(i + 2), 16);
          if (high < 0 || low < 0) return EMPTY;
          decoded.write((high << 4) + low); i += 3;
        } else {
          int point = encoded.codePointAt(i);
          decoded.writeBytes(new String(Character.toChars(point)).getBytes(StandardCharsets.UTF_8));
          i += Character.charCount(point);
        }
        if (decoded.size() > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES) return EMPTY;
      }
      return decoded.toByteArray();
    }

    private List<InetAddress> publicAddresses(URI uri) throws UnknownHostException {
      String scheme = uri.getScheme(), host = uri.getHost();
      if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) || host == null || uri.getUserInfo() != null) return List.of();
      host = normalizeHost(host);
      boolean ipv6 = host.contains(":");
      List<InetAddress> addresses = List.copyOf(dns.lookup(host));
      return !addresses.isEmpty() && !(ipv6 && addresses.stream().anyMatch(Inet4Address.class::isInstance))
              && addresses.stream().allMatch(PdfExternalResourceLoader::isPubliclyRoutable) ? addresses : List.of();
    }

    private List<InetAddress> pinned(String requested, String validated, List<InetAddress> addresses) throws UnknownHostException {
      if (!requested.equalsIgnoreCase(validated)) throw new UnknownHostException("Unvalidated host: " + requested);
      return addresses;
    }
    private int remainingTimeoutMillis() { return (int) Math.max(0, Math.min(REQUEST_TIMEOUT_MILLIS, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()))); }
    private boolean isRedirect(int status) { return Set.of(301, 302, 303, 307, 308).contains(status); }
    private String normalizeHost(String host) { return host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host; }
  }

  static boolean isPubliclyRoutable(InetAddress address) {
    if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
    byte[] b = address.getAddress();
    if (address instanceof Inet4Address) return !prefix(b, 0x00, 8) && !prefix(b, 0x0a, 8) && !prefix(b, 0x6440, 10) && !prefix(b, 0x7f, 8) && !prefix(b, 0xa9fe, 16) && !prefix(b, 0xac10, 12) && !prefix(b, 0xc00000, 24) && !prefix(b, 0xc00002, 24) && !prefix(b, 0xc05863, 24) && !prefix(b, 0xc0a8, 16) && !prefix(b, 0xc612, 15) && !prefix(b, 0xc63364, 24) && !prefix(b, 0xcb0071, 24) && !prefix(b, 0xe0, 4) && !prefix(b, 0xf0, 4);
    if (b.length != 16 || mapped(b) || starts(b, new byte[] {0, 100, (byte) 0xff, (byte) 0x9b})) return false;
    return !prefix(b, 0, 8) && !prefix(b, 0x0100000000000000L, 64) && !prefix(b, 0x200100, 23) && !prefix(b, 0x20010db8, 32) && !prefix(b, 0x2002, 16) && !prefix(b, 0x3ffe, 16) && !prefix(b, 0xfc, 7) && !prefix(b, 0xfec0, 10) && !prefix(b, 0xfe80, 10) && !prefix(b, 0xff, 8);
  }
  private static boolean mapped(byte[] b) { for (int i = 0; i < 10; i++) if (b[i] != 0) return false; return (b[10] == 0 && b[11] == 0) || (b[10] == (byte) 0xff && b[11] == (byte) 0xff); }
  private static boolean starts(byte[] b, byte[] p) { for (int i = 0; i < p.length; i++) if (b[i] != p[i]) return false; return true; }
  private static boolean prefix(byte[] b, long p, int bits) { int whole = bits / 8, rem = bits % 8, count = (bits + 7) / 8; for (int i = 0; i < whole; i++) if ((b[i] & 255) != ((p >> ((count - i - 1) * 8)) & 255)) return false; return rem == 0 || ((b[whole] & 255) & (0xff << (8 - rem))) == ((p & 255) & (0xff << (8 - rem))); }
}
