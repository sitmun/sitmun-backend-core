package org.sitmun.recover.service;

import lombok.extern.slf4j.Slf4j;
import org.sitmun.recover.dto.EmailForgotPassword;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.sitmun.recover.dto.EmailObject;
import org.sitmun.recover.exception.MailNotImplementedException;

/**
 * No-op mail service implementation.
 * Used when mail profile is not active.
 * Throws MailNotImplementedException for all mail operations.
 */
@Service
@Profile("!mail")
@Slf4j
public class NoOpMailService implements MailService {

    @Override
    public void sendEmail(String from, String to, EmailObject emailObject) {
        log.warn("Mail service is not available. Attempted to send email from {} to {}", from, to);
        throw new MailNotImplementedException("Mail service is not available. Please enable the 'mail' profile to use mail features.");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

  @Override
  public EmailForgotPassword buildForgotPasswordEmail(String resetUrl) {
    throw new MailNotImplementedException("Mail service is not available. Please enable the 'mail' profile to use mail features.");
  }
} 