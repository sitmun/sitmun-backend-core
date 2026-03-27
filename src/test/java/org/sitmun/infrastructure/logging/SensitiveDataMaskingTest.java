package org.sitmun.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("SensitiveDataMasking (backend)")
class SensitiveDataMaskingTest {

  /**
   * One row per distinct header name or Authorization shape used in this stack (admin more-info,
   * proxy config, JWT/Basic tests). Do not re-assert these in other test methods.
   */
  static Stream<Arguments> realWorldHeaderCases() {
    return Stream.of(
        Arguments.of(
            "X-API-Key",
            "super-secret",
            SensitiveDataMasking.REDACTED,
            "Admin more-info API task (X-API-Key)"),
        Arguments.of(
            "X-SITMUN-Proxy-Key",
            "middleware-shared-secret",
            SensitiveDataMasking.REDACTED,
            "ProxyMiddlewareConstants.PROXY_MIDDLEWARE_KEY"),
        Arguments.of(
            "Authorization",
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.payload.sig",
            "Bearer " + SensitiveDataMasking.REDACTED,
            "Bearer + JWT (TestUtils / client tests)"),
        Arguments.of(
            "Authorization",
            "bearer lowercase-scheme",
            "Bearer " + SensitiveDataMasking.REDACTED,
            "Case-insensitive Bearer prefix"),
        Arguments.of(
            "Authorization",
            "Basic dGVzdHVzZXI6dGVzdHBhc3M=",
            "Basic " + SensitiveDataMasking.REDACTED,
            "Basic + base64 (proxy decorator)"),
        Arguments.of(
            "Authorization",
            "basic dGVzdA==",
            "Basic " + SensitiveDataMasking.REDACTED,
            "Case-insensitive Basic scheme"),
        Arguments.of(
            "Authorization",
            "Digest username=\"u\"",
            SensitiveDataMasking.REDACTED,
            "Other Authorization schemes fully redacted"),
        Arguments.of(
            "Set-Cookie",
            "SESSION=abc123; Path=/; HttpOnly",
            SensitiveDataMasking.REDACTED,
            "Set-Cookie"),
        Arguments.of("Cookie", "SESSION=abc; other=value", SensitiveDataMasking.REDACTED, "Cookie"),
        Arguments.of(
            "X-Forwarded-For", "203.0.113.1", "203.0.113.1", "Non-sensitive header passes through"),
        Arguments.of(
            "X-CSRF-Token", "csrf-value", SensitiveDataMasking.REDACTED, "Substring token matches"),
        Arguments.of(
            "Accept", "application/json", "application/json", "Typical safe response header"));
  }

  @ParameterizedTest(name = "{3}")
  @MethodSource("realWorldHeaderCases")
  @DisplayName("Masks per real header names and Authorization shapes in the codebase")
  void masksRealWorldHeaderCases(
      String headerName, String rawValue, String expectedMasked, String description) {
    assertThat(SensitiveDataMasking.maskValue(headerName, rawValue)).isEqualTo(expectedMasked);
  }

  @Test
  @DisplayName("maskSortedMap sorts case-insensitively and masks only sensitive keys")
  void maskSortedMapSortsAndMasksSubset() {
    Map<String, String> in = new LinkedHashMap<>();
    in.put("zebra", "z");
    in.put("Alpha", "a");
    in.put("Custom-Api-Key-Client", "hunter2");
    Map<String, String> out = SensitiveDataMasking.maskSortedMap(in);
    assertThat(out.keySet()).containsExactly("Alpha", "Custom-Api-Key-Client", "zebra");
    assertThat(out.get("Alpha")).isEqualTo("a");
    assertThat(out.get("zebra")).isEqualTo("z");
    assertThat(out.get("Custom-Api-Key-Client")).isEqualTo(SensitiveDataMasking.REDACTED);
  }
}
