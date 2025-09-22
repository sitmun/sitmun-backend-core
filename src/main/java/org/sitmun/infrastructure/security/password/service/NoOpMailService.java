package org.sitmun.infrastructure.security.password.service;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.security.password.dto.EmailForgotPassword;
import org.sitmun.infrastructure.security.password.dto.EmailObject;
import org.sitmun.infrastructure.security.password.exception.MailNotImplementedException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-op mail service implementation. Used when mail profile is not active. Throws
 * MailNotImplementedException for all mail operations.
 */
@Service
@Profile("!mail")
@Slf4j
public class NoOpMailService implements MailService {

  @Override
  public void sendEmail(String from, String to, EmailObject emailObject) {
    log.warn("Mail service is not available. Attempted to send email from {} to {}", from, to);
    throw new MailNotImplementedException(
        "Mail service is not available. Please enable the 'mail' profile to use mail features.");
  }

  @Override
  public boolean isAvailable() {
    return false;
  }

  @Override
  public EmailForgotPassword buildForgotPasswordEmail(String resetUrl) {
    throw new MailNotImplementedException(
        "Mail service is not available. Please enable the 'mail' profile to use mail features.");
  }
}
