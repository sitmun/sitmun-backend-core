package org.sitmun.administration.service.template.export;

import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.layout.LayoutContext;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.render.Box;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Converts rendered template HTML to PDF. */
@Service
public class TemplateExportService {

  static final String DEFAULT_PDF_PAGE_SIZE = "A4";
  static final String DEFAULT_PDF_PAGE_ORIENTATION = "portrait";
  private static final Set<String> ALLOWED_PDF_PAGE_SIZES = Set.of("A3", "A4");
  private static final Set<String> ALLOWED_PDF_PAGE_ORIENTATIONS = Set.of("portrait", "landscape");
  private static final int EXTERNAL_RESOURCE_TIMEOUT_MILLIS = 5_000;
  private static final int MAX_EXTERNAL_RESOURCES = 32;
  private static final int MAX_EXTERNAL_RESOURCE_BYTES = 16_000_000;
  private static final int MAX_EXTERNAL_RESOURCE_DURATION_SECONDS = 15;
  private static final int MAX_EXTERNAL_RESOURCE_REDIRECTS = 5;
  private static final byte[] EMPTY_RESOURCE = new byte[0];
  private final Dns dns;
  private final OkHttpClient httpClient;

  public TemplateExportService() {
    this(
        Dns.SYSTEM,
        new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build());
  }

  TemplateExportService(Dns dns, OkHttpClient httpClient) {
    this.dns = dns;
    this.httpClient = httpClient;
  }

  /**
   * Exports rendered HTML to PDF.
   *
   * @param html rendered HTML string, which is mandatory
   * @param output output format; only {@code "pdf"} is supported
   * @param exportTask optional authorized document export task used for output and page settings
   * @return the file content as a byte array
   */
  public byte[] exportHtml(String html, String output, Task exportTask) {
    String normalizedOutput = normalizeOutput(output);
    validateOutput(normalizedOutput);
    validateOutputAllowed(exportTask, normalizedOutput);
    if (!StringUtils.hasText(html)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template HTML is required");
    }
    validateSourceLength(html);
    PdfPageConfig pageConfig = resolvePdfPageConfig(exportTask);
    Document sourceDocument = Jsoup.parse(html);
    validateDocumentComplexity(sourceDocument);
    PdfResourceLoader resourceLoader = new PdfResourceLoader();
    PdfDocumentPreparer.PdfRegionDimensions dimensions =
        measurePdfRegions(sourceDocument.clone(), pageConfig, resourceLoader);
    Document document = PdfDocumentPreparer.prepare(sourceDocument, pageConfig, dimensions);
    return convertHtmlToPdf(document, resourceLoader);
  }

  public String resolveExportFilename(Task templateTask, Task exportTask, String output) {
    String extension = normalizeOutput(output);
    validateOutput(extension);
    Task filenameTask = templateTask != null ? templateTask : exportTask;
    if (filenameTask == null) {
      return "report." + extension;
    }

    String taskName =
        StringUtils.hasText(filenameTask.getName()) ? filenameTask.getName().trim() : "report";

    return sanitizeFilename(taskName) + "." + extension;
  }

  private static void validateOutputAllowed(Task exportTask, String output) {
    if (exportTask == null) {
      return;
    }
    if (!isOutputAllowed(exportTask.getProperties(), output)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Output '" + output + "' is not enabled for task " + exportTask.getId());
    }
  }

  private static boolean isOutputAllowed(Map<String, Object> properties, String output) {
    String normalizedOutput = normalizeOutput(output);
    if (properties != null) {
      Object rawFormat = properties.get(DomainConstants.Tasks.PROPERTY_DOWNLOAD_FORMAT);
      if (rawFormat != null && StringUtils.hasText(String.valueOf(rawFormat))) {
        return normalizedOutput.equals(normalizeOutput(String.valueOf(rawFormat)));
      }
    }

    return false;
  }

  public static String normalizeOutput(String output) {
    return output == null ? "" : output.trim().toLowerCase(Locale.ROOT);
  }

  private static String sanitizeFilename(String value) {
    return value.replaceAll("[\\\\/:*?\"<>|]+", "_");
  }

  PdfPageConfig resolvePdfPageConfig(Task exportTask) {
    if (exportTask == null) {
      return new PdfPageConfig(DEFAULT_PDF_PAGE_SIZE, DEFAULT_PDF_PAGE_ORIENTATION);
    }

    Map<String, Object> properties = exportTask.getProperties();
    return new PdfPageConfig(
        normalizePdfPageSize(
            properties == null ? null : properties.get(DomainConstants.Tasks.PROPERTY_PAGE_SIZE)),
        normalizePdfPageOrientation(
            properties == null
                ? null
                : properties.get(DomainConstants.Tasks.PROPERTY_PAGE_ORIENTATION)));
  }

  static String normalizePdfPageSize(Object value) {
    if (value == null) {
      return DEFAULT_PDF_PAGE_SIZE;
    }
    String normalized = String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    return ALLOWED_PDF_PAGE_SIZES.contains(normalized) ? normalized : DEFAULT_PDF_PAGE_SIZE;
  }

  static String normalizePdfPageOrientation(Object value) {
    if (value == null) {
      return DEFAULT_PDF_PAGE_ORIENTATION;
    }
    String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    return ALLOWED_PDF_PAGE_ORIENTATIONS.contains(normalized)
        ? normalized
        : DEFAULT_PDF_PAGE_ORIENTATION;
  }

  private byte[] convertHtmlToPdf(Document jsoupDoc, PdfResourceLoader resourceLoader) {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfBoxRenderer renderer =
            createRendererBuilder(jsoupDoc, out, resourceLoader).buildPdfRenderer()) {
      renderer.layout();
      validatePageCount(renderer);
      renderer.createPDF();
      return out.toByteArray();
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate PDF", e);
    }
  }

  PdfDocumentPreparer.PdfRegionDimensions measurePdfRegions(String html, PdfPageConfig pageConfig) {
    return measurePdfRegions(
        Jsoup.parse(html == null ? "" : html), pageConfig, new PdfResourceLoader());
  }

  private static void validateOutput(String output) {
    if (!"pdf".equals(output)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported template export output: " + output);
    }
  }

  private static void validateSourceLength(String source) {
    if (source.length() > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_CHARACTERS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export source is too large");
    }
  }

  private static void validateDocumentComplexity(Document document) {
    if (document.getAllElements().size() > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_DOM_ELEMENTS) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export document is too complex");
    }
  }

  private PdfDocumentPreparer.PdfRegionDimensions measurePdfRegions(
      Document sourceDocument, PdfPageConfig pageConfig, PdfResourceLoader resourceLoader) {
    PdfDocumentPreparer.MeasurementDocument measurement =
        PdfDocumentPreparer.prepareForMeasurement(sourceDocument, pageConfig);
    if (!measurement.hasHeader() && !measurement.hasFooter()) {
      return new PdfDocumentPreparer.PdfRegionDimensions(0, 0);
    }

    try (PDDocument pdfDocument = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfBoxRenderer renderer =
            createRendererBuilder(measurement.document(), out, resourceLoader)
                .usePDDocument(pdfDocument)
                .buildPdfRenderer()) {
      renderer.layout();
      validatePageCount(renderer);
      LayoutContext context = renderer.getSharedContext().newLayoutContextInstance();
      double headerHeight =
          measurement.hasHeader()
              ? measureElementHeight(renderer, context, PdfDocumentPreparer.MEASUREMENT_HEADER)
              : 0;
      if (measurement.headerHasTopSpacing()) {
        headerHeight += PdfDocumentPreparer.HEADER_TOP_SPACING_MM;
      }
      double footerHeight =
          measurement.hasFooter()
              ? measureElementHeight(renderer, context, PdfDocumentPreparer.MEASUREMENT_FOOTER)
              : 0;
      return new PdfDocumentPreparer.PdfRegionDimensions(headerHeight, footerHeight);
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to measure PDF header and footer", e);
    }
  }

  private static double measureElementHeight(
      PdfBoxRenderer renderer, LayoutContext context, String regionName) {
    Box box = findMeasurementBox(renderer.getRootBox(), regionName);
    if (box == null) {
      throw new IllegalStateException("PDF region was not laid out: " + regionName);
    }
    return box.getMarginEdge(context, 0, 0).getHeight() * context.getMmPerDot();
  }

  private static Box findMeasurementBox(Box box, String regionName) {
    if (box.getElement() != null
        && regionName.equals(
            box.getElement().getAttribute(PdfDocumentPreparer.MEASUREMENT_ATTRIBUTE))) {
      return box;
    }
    for (Box child : box.getChildren()) {
      Box match = findMeasurementBox(child, regionName);
      if (match != null) {
        return match;
      }
    }
    return null;
  }

  private PdfRendererBuilder createRendererBuilder(
      Document jsoupDoc, ByteArrayOutputStream out, PdfResourceLoader resourceLoader) {
    // Convert HTML5 through Jsoup to avoid the Oracle XML parser rejecting non-XHTML input.
    jsoupDoc.outputSettings().syntax(OutputSettings.Syntax.xml);
    org.w3c.dom.Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);
    PdfRendererBuilder builder = new PdfRendererBuilder();
    builder.useFastMode();
    builder.useHttpStreamImplementation(resourceLoader::load);
    builder.useExternalResourceAccessControl(
        (uri, resourceType) -> isAllowedResource(uri),
        ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI);
    builder.withW3cDocument(w3cDoc, null);
    builder.toStream(out);
    return builder;
  }

  boolean isAllowedResource(String uri) {
    if (uri == null) {
      return false;
    }
    try {
      String scheme = URI.create(uri).getScheme();
      return "data".equalsIgnoreCase(scheme)
          || "http".equalsIgnoreCase(scheme)
          || "https".equalsIgnoreCase(scheme);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  static boolean isPubliclyRoutable(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) {
      return false;
    }

    byte[] bytes = address.getAddress();
    if (address instanceof Inet4Address) {
      return !hasPrefix(bytes, 0x00, 8)
          && !hasPrefix(bytes, 0x0a, 8)
          && !hasPrefix(bytes, 0x6440, 10)
          && !hasPrefix(bytes, 0x7f, 8)
          && !hasPrefix(bytes, 0xa9fe, 16)
          && !hasPrefix(bytes, 0xac10, 12)
          && !hasPrefix(bytes, 0xc00000, 24)
          && !hasPrefix(bytes, 0xc00002, 24)
          && !hasPrefix(bytes, 0xc05863, 24)
          && !hasPrefix(bytes, 0xc0a8, 16)
          && !hasPrefix(bytes, 0xc612, 15)
          && !hasPrefix(bytes, 0xc63364, 24)
          && !hasPrefix(bytes, 0xcb0071, 24)
          && !hasPrefix(bytes, 0xe0, 4)
          && !hasPrefix(bytes, 0xf0, 4);
    }

    if (bytes.length != 16 || isIpv4MappedOrCompatible(bytes) || isNat64Address(bytes)) {
      return false;
    }
    return !hasPrefix(bytes, 0x00, 8)
        && !hasPrefix(bytes, 0x0100000000000000L, 64)
        && !hasPrefix(bytes, 0x200100, 23)
        && !hasPrefix(bytes, 0x20010db8, 32)
        && !hasPrefix(bytes, 0x2002, 16)
        && !hasPrefix(bytes, 0x3ffe, 16)
        && !hasPrefix(bytes, 0xfc, 7)
        && !hasPrefix(bytes, 0xfec0, 10)
        && !hasPrefix(bytes, 0xfe80, 10)
        && !hasPrefix(bytes, 0xff, 8);
  }

  private static boolean isIpv4MappedOrCompatible(byte[] bytes) {
    for (int i = 0; i < 10; i++) {
      if (bytes[i] != 0) {
        return false;
      }
    }
    return (bytes[10] == 0 && bytes[11] == 0)
        || (bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff);
  }

  private static boolean isNat64Address(byte[] bytes) {
    byte[] wellKnownPrefix = {0x00, 0x64, (byte) 0xff, (byte) 0x9b, 0, 0, 0, 0, 0, 0, 0, 0};
    byte[] localPrefix = {0x00, 0x64, (byte) 0xff, (byte) 0x9b, 0x00, 0x01};
    return startsWith(bytes, wellKnownPrefix) || startsWith(bytes, localPrefix);
  }

  private static boolean startsWith(byte[] value, byte[] prefix) {
    for (int i = 0; i < prefix.length; i++) {
      if (value[i] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasPrefix(byte[] address, long prefix, int bits) {
    int wholeBytes = bits / 8;
    int remainingBits = bits % 8;
    int prefixBytes = (bits + 7) / 8;
    for (int i = 0; i < wholeBytes; i++) {
      int shift = (prefixBytes - i - 1) * 8;
      if ((address[i] & 0xff) != ((prefix >> shift) & 0xff)) {
        return false;
      }
    }
    if (remainingBits == 0) {
      return true;
    }
    int mask = (0xff << (8 - remainingBits)) & 0xff;
    return ((address[wholeBytes] & 0xff) & mask) == ((prefix & 0xff) & mask);
  }

  private static void validatePageCount(PdfBoxRenderer renderer) {
    int pageCount = renderer.getRootBox().getLayer().getPages().size();
    if (pageCount > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_PDF_PAGES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Export PDF has too many pages");
    }
  }

  private final class PdfResourceLoader {
    private final Map<String, byte[]> cache = new HashMap<>();
    private final long deadlineNanos =
        System.nanoTime() + TimeUnit.SECONDS.toNanos(MAX_EXTERNAL_RESOURCE_DURATION_SECONDS);
    private int totalBytes;

    private synchronized FSStream load(String uri) {
      byte[] cached = cache.get(uri);
      if (cached != null) {
        return new ByteArrayResourceStream(cached);
      }
      if (!isAllowedResource(uri)
          || cache.size() >= MAX_EXTERNAL_RESOURCES
          || System.nanoTime() >= deadlineNanos) {
        return new ByteArrayResourceStream(EMPTY_RESOURCE);
      }

      byte[] content = fetch(uri);
      if (content.length > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES
          || totalBytes + content.length > MAX_EXTERNAL_RESOURCE_BYTES) {
        content = EMPTY_RESOURCE;
      }
      totalBytes += content.length;
      cache.put(uri, content);
      return new ByteArrayResourceStream(content);
    }

    private byte[] fetch(String uri) {
      try {
        URI current = URI.create(uri);
        if ("data".equalsIgnoreCase(current.getScheme())) {
          return decodeDataResource(uri);
        }
        for (int redirects = 0; redirects <= MAX_EXTERNAL_RESOURCE_REDIRECTS; redirects++) {
          int timeout = remainingTimeoutMillis();
          List<InetAddress> addresses = resolvePublicAddresses(current);
          if (timeout <= 0 || addresses.isEmpty()) {
            return EMPTY_RESOURCE;
          }

          String validatedHost = normalizeHost(current.getHost());
          OkHttpClient pinnedClient =
              httpClient
                  .newBuilder()
                  .dns(host -> pinnedAddresses(host, validatedHost, addresses))
                  .proxy(Proxy.NO_PROXY)
                  .followRedirects(false)
                  .followSslRedirects(false)
                  .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                  .readTimeout(timeout, TimeUnit.MILLISECONDS)
                  .callTimeout(timeout, TimeUnit.MILLISECONDS)
                  .build();
          Request request = new Request.Builder().url(current.toString()).get().build();
          try (Response response = pinnedClient.newCall(request).execute()) {
            if (isRedirect(response.code())) {
              if (redirects == MAX_EXTERNAL_RESOURCE_REDIRECTS) {
                return EMPTY_RESOURCE;
              }
              String location = response.header("Location");
              if (location == null) {
                return EMPTY_RESOURCE;
              }
              current = current.resolve(location);
              continue;
            }
            if (!response.isSuccessful()) {
              return EMPTY_RESOURCE;
            }
            ResponseBody body = response.body();
            if (body == null) {
              return EMPTY_RESOURCE;
            }
            try (var input = body.byteStream()) {
              return input.readNBytes(
                  Math.min(
                          DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES,
                          MAX_EXTERNAL_RESOURCE_BYTES - totalBytes)
                      + 1);
            }
          }
        }
      } catch (IOException | RuntimeException exception) {
        return EMPTY_RESOURCE;
      }
      return EMPTY_RESOURCE;
    }

    private byte[] decodeDataResource(String uri) {
      int separator = uri.indexOf(',');
      if (separator < 0) {
        return EMPTY_RESOURCE;
      }
      String encoded = uri.substring(separator + 1);
      boolean base64 = uri.substring(0, separator).toLowerCase(Locale.ROOT).endsWith(";base64");
      long maxEncodedLength =
          base64
              ? ((long) DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES + 2) / 3 * 4
              : (long) DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES * 3;
      if (encoded.length() > maxEncodedLength) {
        return EMPTY_RESOURCE;
      }
      try {
        return base64
            ? Base64.getDecoder().decode(encoded)
            : decodePercentEncodedData(encoded);
      } catch (IllegalArgumentException exception) {
        return EMPTY_RESOURCE;
      }
    }

    private byte[] decodePercentEncodedData(String encoded) {
      ByteArrayOutputStream decoded = new ByteArrayOutputStream();
      for (int index = 0; index < encoded.length(); ) {
        char current = encoded.charAt(index);
        if (current == '%') {
          if (index + 2 >= encoded.length()) {
            return EMPTY_RESOURCE;
          }
          int high = Character.digit(encoded.charAt(index + 1), 16);
          int low = Character.digit(encoded.charAt(index + 2), 16);
          if (high < 0 || low < 0) {
            return EMPTY_RESOURCE;
          }
          decoded.write((high << 4) + low);
          index += 3;
        } else {
          int codePoint = encoded.codePointAt(index);
          decoded.writeBytes(
              new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8));
          index += Character.charCount(codePoint);
        }
        if (decoded.size() > DomainConstants.Tasks.MAX_TEMPLATE_EXPORT_SOURCE_BYTES) {
          return EMPTY_RESOURCE;
        }
      }
      return decoded.toByteArray();
    }

    private List<InetAddress> resolvePublicAddresses(URI uri) throws UnknownHostException {
      String scheme = uri.getScheme();
      String host = uri.getHost();
      if (!("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
          || host == null
          || uri.getUserInfo() != null) {
        return List.of();
      }
      host = normalizeHost(host);
      boolean ipv6Literal = host.contains(":");
      List<InetAddress> addresses = List.copyOf(dns.lookup(host));
      return !addresses.isEmpty()
              && !(ipv6Literal && addresses.stream().anyMatch(Inet4Address.class::isInstance))
              && addresses.stream().allMatch(TemplateExportService::isPubliclyRoutable)
          ? addresses
          : List.of();
    }

    private String normalizeHost(String host) {
      return host.startsWith("[") && host.endsWith("]")
          ? host.substring(1, host.length() - 1)
          : host;
    }

    private List<InetAddress> pinnedAddresses(
        String requestedHost, String validatedHost, List<InetAddress> addresses)
        throws UnknownHostException {
      if (!requestedHost.equalsIgnoreCase(validatedHost)) {
        throw new UnknownHostException("Unvalidated host: " + requestedHost);
      }
      return addresses;
    }

    private boolean isRedirect(int status) {
      return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private int remainingTimeoutMillis() {
      long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime());
      return (int) Math.max(0, Math.min(EXTERNAL_RESOURCE_TIMEOUT_MILLIS, remainingMillis));
    }
  }

  byte[] fetchExternalResource(String uri) {
    return new PdfResourceLoader().fetch(uri);
  }

  private record ByteArrayResourceStream(byte[] content) implements FSStream {
    @Override
    public ByteArrayInputStream getStream() {
      return new ByteArrayInputStream(content);
    }

    @Override
    public Reader getReader() {
      return new InputStreamReader(getStream(), StandardCharsets.UTF_8);
    }
  }

  record PdfPageConfig(String pageSize, String pageOrientation) {}
}
