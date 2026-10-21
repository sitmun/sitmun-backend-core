package org.sitmun.infrastructure.security.password.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CodeOTPServiceTest {

  @Test
  @DisplayName("Reset code is eight digits drawn from SecureRandom")
  void createCodeOTPUsesSecureRandom() throws Exception {
    CodeOTPService service = new CodeOTPService();
    Field field = CodeOTPService.class.getDeclaredField("random");
    field.setAccessible(true);

    assertThat(field.get(null)).isInstanceOf(SecureRandom.class);
    assertThat(service.createCodeOTP()).matches("\\d{8}");
  }

  @Test
  @DisplayName("OTP hash stays SHA-256 of the eight-digit code")
  void hashCodeOTPStaysSha256() {
    assertThat(new CodeOTPService().hashCodeOTP("00000042"))
        .isEqualTo("nrKNeL8DH1hfonzIqG1BfgUM+KCckz/rc7v9x1ZWhWE=");
  }
}
