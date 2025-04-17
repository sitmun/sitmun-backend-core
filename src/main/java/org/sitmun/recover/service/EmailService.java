package org.sitmun.recover.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sitmun.recover.dto.EmailObject;


@Service
public class EmailService {

  @Autowired
  private JavaMailSender mailSender;

  private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

  public boolean sendEmail(String from, String to, EmailObject emailObject) {
    boolean sendEmailSucess = false;
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(from);
      message.setTo(to);
      message.setSubject(emailObject.getSubject());
      message.setText(emailObject.getBody());

      mailSender.send(message);
      sendEmailSucess = true;
    }
    catch (Exception e) {
      logger.error("Failed to send email", e);
    }

    return sendEmailSucess;
  }
}
