package org.sitmun.infrastructure.security.password.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.infrastructure.config.Profiles;
import org.sitmun.infrastructure.security.password.dto.EmailForgotPassword;
import org.sitmun.infrastructure.security.password.dto.EmailObject;
import org.sitmun.infrastructure.security.password.exception.EmailTemplateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Profile(Profiles.MAIL)
@Slf4j
public class EmailService implements MailService {

  private final JavaMailSender mailSender;

  private final TemplateEngine templateEngine;

  @Value("${spring.mail.forgot-password.subject}")
  private String forgotPasswordSubject;

  @Value("${spring.mail.forgot-password.body-template}")
  private String forgotPasswordBodyTemplate;

  public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
    this.mailSender = mailSender;
    this.templateEngine = templateEngine;
  }

  @Override
  public void sendEmail(String from, String to, EmailObject emailObject) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(to);
    message.setSubject(emailObject.getSubject());
    message.setText(emailObject.getBody());

    mailSender.send(message);
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  public EmailForgotPassword buildForgotPasswordEmail(String codeOTP) {
    String body = renderForgotPasswordBody(Map.of("codeOTP", codeOTP));
    return new EmailForgotPassword(forgotPasswordSubject, body);
  }

  private String renderForgotPasswordBody(Map<String, Object> variables) {
    try {
      Context context = new Context();
      context.setVariables(variables);
      return templateEngine.process(forgotPasswordBodyTemplate, context);
    } catch (Exception e) {
      log.error("Failed to render forgot password email body", e);
      throw new EmailTemplateException("Failed to render forgot password email body", e);
    }
  }
}
