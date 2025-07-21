package org.sitmun.recover.controller;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

@TestConfiguration
public class MockMailConfig {
  @Bean
  public JavaMailSender javaMailSender() {
    return Mockito.mock(JavaMailSender.class);
  }
}
