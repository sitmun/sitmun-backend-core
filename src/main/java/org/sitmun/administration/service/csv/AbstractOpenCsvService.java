package org.sitmun.administration.service.csv;

import com.opencsv.*;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import org.springframework.util.StringUtils;

public abstract class AbstractOpenCsvService {

  protected static final char CSV_SEPARATOR = ',';
  protected static final char CSV_QUOTE = '"';
  protected static final String CSV_LINE_END = "\r\n";
  protected static final String UTF_8_BOM = "\uFEFF";

  protected CSVReader createReader(Reader reader) {
    CSVParser parser =
        new CSVParserBuilder()
            .withSeparator(CSV_SEPARATOR)
            .withQuoteChar(CSV_QUOTE)
            .withEscapeChar(ICSVParser.DEFAULT_ESCAPE_CHARACTER)
            .withStrictQuotes(false)
            .build();
    return new CSVReaderBuilder(reader).withCSVParser(parser).build();
  }

  protected CSVWriter createWriter(Writer writer) {
    return new CSVWriter(
        writer, CSV_SEPARATOR, CSV_QUOTE, ICSVWriter.DEFAULT_ESCAPE_CHARACTER, CSV_LINE_END);
  }

  protected String stripBom(String value) {
    if (value == null) {
      return null;
    }
    return value.startsWith(UTF_8_BOM) ? value.substring(1) : value;
  }

  protected String normalizeRequiredCode(String value) {
    String normalized = stripBom(value);
    if (!StringUtils.hasText(normalized)) {
      return null;
    }
    return normalized.trim();
  }

  protected String normalizeLiteral(String value) {
    String normalized = stripBom(value);
    return StringUtils.hasText(normalized) ? normalized : null;
  }

  protected byte[] withUtf8Bom(String csv) {
    byte[] body = csv.getBytes(StandardCharsets.UTF_8);
    byte[] bytes = new byte[body.length + 3];
    bytes[0] = (byte) 0xEF;
    bytes[1] = (byte) 0xBB;
    bytes[2] = (byte) 0xBF;
    System.arraycopy(body, 0, bytes, 3, body.length);
    return bytes;
  }
}
