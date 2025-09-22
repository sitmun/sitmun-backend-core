package org.sitmun.infrastructure.security.password.controller;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.security.password.dto.EmailForgotPassword;
import org.sitmun.infrastructure.security.password.dto.EmailObject;
import org.sitmun.infrastructure.security.password.service.MailService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Test implementation of MailService that doesn't actually send emails.
 * Used for testing password recovery functionality.
 */
@Service
@Profile("mail")
@Primary
@Slf4j
public class TestMailService implements MailService {

  @Override
  public void sendEmail(String from, String to, EmailObject emailObject) {
    log.info("Test: Would send email from {} to {} with subject: {}", from, to, emailObject.getSubject());
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public EmailForgotPassword buildForgotPasswordEmail(String codeOTP) {
    return new EmailForgotPassword("Password Recovery - SITMUN", "Your recovery code is: " + codeOTP);
  }
}
