package org.sitmun.authorization.proxy.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
@Builder
public class HttpSecurityDto {

  private static final String OPENAPI_TYPE_HTTP = "http";
  private static final String OPENAPI_TYPE_API_KEY = "apiKey";

  private String type;

  private String scheme;

  private String username;

  private String password;

  private Map<String, String> headers;

  private Map<String, String> queryParams;

  /**
   * Debug-oriented summary: {@code apiKey} → header names (and username if erroneously set); {@code
   * http} (including legacy blank type with credentials) → scheme, username (literal when set), and
   * password presence; other types → same plus header names. Passwords and header values are never
   * logged. Mismatched fields add {@code warn=[...]}.
   */
  public String describeForLog() {
    if (isApiKeyType()) {
      return describeApiKeyForLog();
    }
    if (isHttpTypeForLog()) {
      return describeHttpForLog();
    }
    return describeOtherForLog();
  }

  private String describeApiKeyForLog() {
    List<String> warns = new ArrayList<>();
    if (StringUtils.hasText(scheme)) {
      warns.add("apiKeyWithScheme");
    }
    if (StringUtils.hasText(username)) {
      warns.add("apiKeyWithUsername");
    }
    if (StringUtils.hasText(password)) {
      warns.add("apiKeyWithPassword");
    }
    String base = "type=" + nullToLog(type) + ", headerNames=" + formatHeaderNameList();
    if (StringUtils.hasText(username)) {
      base += ", username=" + username;
    }
    return appendWarns(base, warns);
  }

  private String describeHttpForLog() {
    List<String> warns = new ArrayList<>();
    if (hasHeaders()) {
      warns.add("httpWithHeaders");
    }
    String base =
        "type="
            + nullToLog(type)
            + ", scheme="
            + nullToLog(scheme)
            + ", username="
            + usernameForLog(username)
            + ", password="
            + presence(password);
    return appendWarns(base, warns);
  }

  private String describeOtherForLog() {
    List<String> warns = new ArrayList<>();
    if (!StringUtils.hasText(type) && hasHeaders() && !hasCredentialFields()) {
      warns.add("headersWithoutType");
    }
    String base =
        "type="
            + nullToLog(type)
            + ", scheme="
            + nullToLog(scheme)
            + ", username="
            + usernameForLog(username)
            + ", password="
            + presence(password)
            + ", headerNames="
            + formatHeaderNameList();
    return appendWarns(base, warns);
  }

  private boolean isApiKeyType() {
    return StringUtils.hasText(type) && OPENAPI_TYPE_API_KEY.equalsIgnoreCase(type.trim());
  }

  /** {@code type=http} or legacy payloads with credentials and no explicit type. */
  private boolean isHttpTypeForLog() {
    if (isApiKeyType()) {
      return false;
    }
    if (StringUtils.hasText(type) && OPENAPI_TYPE_HTTP.equalsIgnoreCase(type.trim())) {
      return true;
    }
    return !StringUtils.hasText(type) && hasCredentialFields();
  }

  private static String nullToLog(String s) {
    return s == null ? "null" : s;
  }

  private static String usernameForLog(String value) {
    return StringUtils.hasText(value) ? value : "unset";
  }

  private static String presence(String value) {
    return StringUtils.hasText(value) ? "set" : "unset";
  }

  private boolean hasCredentialFields() {
    return StringUtils.hasText(username) || StringUtils.hasText(password);
  }

  private boolean hasHeaders() {
    return headers != null && !headers.isEmpty();
  }

  private String formatHeaderNameList() {
    if (headers == null || headers.isEmpty()) {
      return "[]";
    }
    return "[" + headers.keySet().stream().sorted().collect(Collectors.joining(", ")) + "]";
  }

  private static String appendWarns(String base, List<String> warns) {
    if (warns.isEmpty()) {
      return base;
    }
    warns.sort(String::compareTo);
    return base + ", warn=[" + String.join(", ", warns) + "]";
  }
}
