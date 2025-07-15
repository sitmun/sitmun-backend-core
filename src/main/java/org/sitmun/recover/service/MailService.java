package org.sitmun.recover.service;

import org.sitmun.recover.dto.EmailForgotPassword;
import org.sitmun.recover.dto.EmailObject;

/**
 * Mail service interface for sending emails.
 * Implementations can be either real mail service or no-op service.
 */
public interface MailService {
    
    /**
     * Send an email.
     * 
     * @param from the sender email address
     * @param to the recipient email address
     * @param emailObject the email content
     */
    void sendEmail(String from, String to, EmailObject emailObject);
    
    /**
     * Check if mail service is available.
     * 
     * @return true if mail service is available, false otherwise
     */
    boolean isAvailable();

  EmailForgotPassword buildForgotPasswordEmail(String resetUrl);
} 