package org.sitmun.recover.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Setter
@Getter
public class EmailForgotPassword implements EmailObject {

    private String subject;
    private String body;

    public EmailForgotPassword(String token, String front_url) {
        setSubject();
        setBody(token, front_url);
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getBody() {
        return body;
    }

    private void setSubject() {
        this.subject = "Password Recovery Link";
    }

    private void setBody(String token, String front_url) {
        String url = front_url + "/auth/forgot-password/" + token;
        this.body = "We received a request to reset your password. If you made this request, please click the link below to set a new password:";
        this.body += "\n" + url + "\n";
        this.body += "If you did not request a password reset, please ignore this message. For your security, the link will expire after a certain period.";
    }
}
