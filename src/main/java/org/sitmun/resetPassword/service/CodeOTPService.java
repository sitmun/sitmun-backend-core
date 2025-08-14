package org.sitmun.resetPassword.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CodeOTPService {
  public String CreateCodeOTP() {
    int randomNum = (int) (Math.random() * 100_000_000);
    return String.format("%08d", randomNum);
  }

  public String HashCodeOTP(String codeOTP) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashedBytes = digest.digest(codeOTP.getBytes());
      // Encodage en base64 pour rendre le hash lisible
      return Base64.getEncoder().encodeToString(hashedBytes);
    } catch (NoSuchAlgorithmException e) {
      log.error("Erreur lors du hashing du OTP", e);
      return null;
    }
  }

  /**
   * Vérifie si le code OTP est valide
   *
   * @param inputCode code OTP à hash
   * @param hashedCode code OTP déjà hash
   * @return
   */
  public boolean ValidateCodeOTP(String inputCode, String hashedCode) {
    String hashedInput = this.HashCodeOTP(inputCode);
    return hashedInput != null && hashedInput.equals(hashedCode);
  }
}
