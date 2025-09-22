package org.sitmun.infrastructure.security.password.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user.token.UserTokenDTO;
import org.sitmun.domain.user.token.UserTokenService;
import org.sitmun.infrastructure.security.password.dto.EmailForgotPassword;
import org.sitmun.infrastructure.security.password.dto.RequestNewPassword;
import org.sitmun.infrastructure.security.password.dto.ResetPasswordRequest;
import org.sitmun.infrastructure.security.password.exception.EmailTemplateException;
import org.sitmun.infrastructure.security.password.exception.MailNotImplementedException;
import org.sitmun.infrastructure.security.password.service.CodeOTPService;
import org.sitmun.infrastructure.security.password.service.MailService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.Optional;

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
  public ResponseEntity<String> requestNewPassword(
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

        String codeOTP = this.codeOTPService.createCodeOTP();
        String hashedCodeOTP = this.codeOTPService.hashCodeOTP(codeOTP);
        long currentTimeMillis = new Date().getTime();
        UserTokenDTO userTokenDTO = UserTokenDTO.builder()
          .userID(user.getId())
          .codeOTP(hashedCodeOTP)
          .expireAt(new Date(currentTimeMillis + recoveryValidity))
          .attemptCounter(0)
          .active(true)
          .build();
        userTokenService.saveUserToken(userTokenDTO);
        EmailForgotPassword emailContent = mailService.buildForgotPasswordEmail(codeOTP);
        mailService.sendEmail(from, email, emailContent);
      }
      return ResponseEntity.status(HttpStatus.OK).body("Mail sent");
    } catch (MailNotImplementedException e) {
      log.error("Mail service not available", e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body("Mail service is not available. Please enable the 'mail' profile to use password recovery.");
    } catch (EmailTemplateException e) {
      log.error("Email template rendering failed", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Server error: Failed to render forgot password email body");
    }
  }

  @PostMapping("/confirm")
  @SecurityRequirements
  public ResponseEntity<String> resetPassword(
      @Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
    User user = this.userRepository.findByEmail(resetPasswordRequest.getEmail()).orElseThrow(
      () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
    );

    UserTokenDTO userToken = userTokenService.getUserTokenByUserId(user.getId());
    if (userToken == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token not found");
    }

    if (!userToken.isActive()) {
      throw new ResponseStatusException(HttpStatus.GONE, "Token has been deactivated");
    }

    if (userToken.getAttemptCounter() >= this.maxAttempt
      || new Date().after(userToken.getExpireAt())) {
      userToken.setActive(false);
      this.userTokenService.updateUserToken(userToken);
      return ResponseEntity.status(HttpStatus.GONE).build();
    }

    if (!resetPasswordRequest.getEmail().equalsIgnoreCase(user.getEmail())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email does not match");
    }

    if (!this.codeOTPService.validateCodeOTP(resetPasswordRequest.getCodeOTP(), userToken.getCodeOTP())) {
      userToken.setAttemptCounter(userToken.getAttemptCounter() + 1);
      this.userTokenService.updateUserToken(userToken);
      int attemptsRemaining = maxAttempt - userToken.getAttemptCounter();

      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(Integer.toString(attemptsRemaining));
    }

    String newPassword = resetPasswordRequest.getNewPassword();
    user.setPassword(newPassword);

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
  }

  @PostMapping("/resend")
  @SecurityRequirements
  public ResponseEntity<String> resendCodeOTP(@Valid @RequestBody RequestNewPassword requestNewPassword) {
    return this.requestNewPassword(requestNewPassword);
  }
}
