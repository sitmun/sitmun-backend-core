package org.sitmun.resetPassword.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.Date;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user_token.UserTokenDTO;
import org.sitmun.domain.user_token.UserTokenService;
import org.sitmun.resetPassword.dto.EmailForgotPassword;
import org.sitmun.resetPassword.dto.RequestNewPassword;
import org.sitmun.resetPassword.dto.ResetPasswordRequest;
import org.sitmun.resetPassword.exception.EmailTemplateException;
import org.sitmun.resetPassword.exception.MailNotImplementedException;
import org.sitmun.resetPassword.service.CodeOTPService;
import org.sitmun.resetPassword.service.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Controller to authenticate users. */
@Slf4j
@RestController
@RequestMapping("/api/password-reset")
@Tag(name = "ResetPassword", description = "Reset the password of the user")
@Profile("mail")
@Validated
public class ResetPasswordController {
  @Value("${sitmun.recover-password.code-otp.max-attempt}")
  private int maxAttempt;

  @Value("${sitmun.recover-password.mail.from}")
  private String from;

  @Value("${sitmun.recover-password.jwt.token-recovery-password-validity-in-milliseconds}")
  private int recoveryValidity;

  private final UserTokenService userTokenService;

  private final SpringValidatorAdapter springValidator;
  private final MailService mailService;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher publisher;
  private final CodeOTPService codeOTPService;

  public ResetPasswordController(
      MailService mailService,
      UserRepository userRepository,
      Validator validator,
      ApplicationEventPublisher publisher,
      CodeOTPService codeOtpService,
      UserTokenService userTokenService) {
    this.mailService = mailService;
    this.userRepository = userRepository;
    this.publisher = publisher;
    this.springValidator = new SpringValidatorAdapter(validator);
    this.userTokenService = userTokenService;
    this.codeOTPService = codeOtpService;
  }

  @PostMapping("/request")
  @SecurityRequirements
  public ResponseEntity<String> RequestNewPassword(
      @Valid @RequestBody RequestNewPassword requestNewPassword) {
    if (!mailService.isAvailable()) {
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              "Mail service is not available. Please enable the 'mail' profile to use password recovery.");
    }

    try {
      String email = requestNewPassword.getEmail();
      Optional<User> optionalUser = this.userRepository.findByEmail(email); // Does user mail exist
      User user = null;
      if (optionalUser.isPresent()) {
        user = optionalUser.get();
      }

      if (user != null && !user.getBlocked() && email != null && !email.trim().isEmpty()) {
        if (this.userTokenService.getUserTokenByUserId(user.getId()) != null) {
          userTokenService.deleteUserTokenByUserId(user.getId());
        }

        String codeOTP = this.codeOTPService.CreateCodeOTP();
        String hashedCodeOTP = this.codeOTPService.HashCodeOTP(codeOTP);
        long currentTimeMillis = new Date().getTime();
        UserTokenDTO userTokenDTO =
            new UserTokenDTO(
                null,
                user.getId(),
                hashedCodeOTP,
                new Date(currentTimeMillis + recoveryValidity),
                0,
                true);
        userTokenService.saveUserToken(userTokenDTO);
        EmailForgotPassword emailContent = mailService.buildForgotPasswordEmail(codeOTP);
        mailService.sendEmail(from, email, emailContent);
      }
      return ResponseEntity.status(HttpStatus.OK).body("Mail sent");
    } catch (MailNotImplementedException e) {
      log.error("Mail service not available", e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .body(
              "Mail service is not available. Please enable the 'mail' profile to use password recovery.");
    } catch (EmailTemplateException e) {
      log.error("Email template rendering failed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Server error: Failed to render forgot password email body");
    } catch (Exception e) {
      log.error("Unexpected error during password recovery", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Server error: " + e.getMessage());
    }
  }

  @PostMapping("/confirm")
  @SecurityRequirements
  public ResponseEntity<String> ResetPassword(
      @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
    try {
      // check if user exist
      Optional<User> storedUser = this.userRepository.findByEmail(resetPasswordRequest.getEmail());
      if (storedUser.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }
      User user = storedUser.get();

      // Check if user userToken exist
      UserTokenDTO userToken = userTokenService.getUserTokenByUserId(user.getId());
      if (userToken == null) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }

      if (!userToken.isActive()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
      }

      // Token already used or expired or attempts exceeded
      if (userToken.getAttemptCounter() >= this.maxAttempt
          || new Date().after(userToken.getExpireAt())) {
        userToken.setActive(false);
        this.userTokenService.updateUserToken(userToken);
        return ResponseEntity.status(HttpStatus.GONE).build();
      }

      // Check if email valid
      if (!resetPasswordRequest.getEmail().equalsIgnoreCase(user.getEmail())) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }

      // Check the code OTP
      if (!this.codeOTPService.ValidateCodeOTP(
          resetPasswordRequest.getCodeOTP(), userToken.getCodeOTP())) {
        userToken.setAttemptCounter(userToken.getAttemptCounter() + 1);
        this.userTokenService.updateUserToken(userToken);
        int attemptsRemaining = maxAttempt - userToken.getAttemptCounter();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Integer.toString(attemptsRemaining));
      }

      // Update password
      String newPassword = resetPasswordRequest.getNewPassword();
      user.setPassword(newPassword);

      // Verify password
      BindingResult bindingResult = new BeanPropertyBindingResult(user, "user");
      this.springValidator.validate(user, bindingResult);
      if (bindingResult.hasErrors()) {
        throw new RepositoryConstraintViolationException(bindingResult);
      }

      userToken.setActive(false);
      this.userTokenService.updateUserToken(userToken);
      user.setLastPasswordChange(new Date());
      this.publisher.publishEvent(new BeforeSaveEvent(user));
      user = this.userRepository.save(user);
      this.publisher.publishEvent(new AfterSaveEvent(user));

      return ResponseEntity.status(HttpStatus.OK).body("Password reset successfully");
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error");
    }
  }

  @PostMapping("/resend")
  @SecurityRequirements
  public ResponseEntity<String> ResendCodeOTP(
      HttpServletRequest request, @Valid @RequestBody RequestNewPassword requestNewPassword) {
    return this.RequestNewPassword(requestNewPassword);
  }
}
