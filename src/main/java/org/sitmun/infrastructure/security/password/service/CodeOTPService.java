package org.sitmun.infrastructure.security.password.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CodeOTPService {

  private static final SecureRandom random = new SecureRandom();

  public String createCodeOTP() {
    int randomNum = random.nextInt(100_000_000);
    return String.format("%08d", randomNum);
  }

  public String hashCodeOTP(String codeOTP) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashedBytes = digest.digest(codeOTP.getBytes());
      return Base64.getEncoder().encodeToString(hashedBytes);
    } catch (NoSuchAlgorithmException e) {
      log.error("Erreur lors du hashing du OTP", e);
      return null;
    }
  }

  /**
   * Validate the input OTP code against the stored hashed code.
   *
   * @param inputCode the OTP code provided by the user
   * @param hashedCode the stored hashed OTP code
   * @return true if the input code matches the hashed code, false otherwise
   */
  public boolean validateCodeOTP(String inputCode, String hashedCode) {
    String hashedInput = this.hashCodeOTP(inputCode);
    return hashedInput != null && hashedInput.equals(hashedCode);
  }
}
