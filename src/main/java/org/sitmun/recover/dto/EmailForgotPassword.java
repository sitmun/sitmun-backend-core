package org.sitmun.recover.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EmailForgotPassword implements EmailObject {
  private String subject;
  private String body;

  public EmailForgotPassword(String subject, String body) {
    this.subject = subject;
    this.body = body;
  }

  @Override
  public String getSubject() {
    return subject;
  }

  @Override
  public String getBody() {
    return body;
  }
}
