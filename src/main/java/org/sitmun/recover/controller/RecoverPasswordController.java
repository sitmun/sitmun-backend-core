package org.sitmun.recover.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.domain.user.User;
import org.sitmun.domain.user.UserRepository;
import org.sitmun.domain.user_token.UserTokenRepository;
import org.sitmun.infrastructure.security.service.JsonWebTokenService;
import org.sitmun.recover.dto.EmailForgotPassword;
import org.sitmun.recover.dto.ResetPasswordRequest;
import org.sitmun.recover.dto.UserLoginRecoverRequest;
import org.sitmun.recover.service.EmailService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import javax.validation.Valid;
import javax.validation.Validator;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.sitmun.domain.user_token.UserTokenService;
import org.sitmun.domain.user_token.UserTokenDTO;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller to authenticate users.
 */
@Slf4j
@RestController
@RequestMapping("/api/recover-password")
@Tag(name = "PasswordRecover", description = "Recover the password of the user")
public class RecoverPasswordController {

  @Value("${front.url}")
  private String frontUrl;

  @Value("${spring.mail.from}")
  private String from;

  @Value("${security.authentication.jwt.token-recovery-password-validity-in-miliseconds}")
  private int recovery_validity;

  @Autowired
  private UserTokenService userTokenService;

  private final SpringValidatorAdapter springValidator;
  private final EmailService passwordRecoveryService;
  private final JsonWebTokenService jsonWebTokenService;
  private final UserRepository userRepository;
  private final ApplicationEventPublisher publisher;

  public RecoverPasswordController(
    EmailService emailService,
    JsonWebTokenService jsonWebTokenService,
    UserRepository userRepository,
    Validator validator,
    ApplicationEventPublisher publisher
  ) {
    this.passwordRecoveryService = emailService;
    this.jsonWebTokenService = jsonWebTokenService;
    this.userRepository = userRepository;
    this.publisher = publisher;
    this.springValidator = new SpringValidatorAdapter(validator);
  }

  @PostMapping
  @SecurityRequirements
  public ResponseEntity<String> sendEmailUser(@Valid @RequestBody UserLoginRecoverRequest body) {
    try {
      String login = body.getLogin();
      boolean isLoginExist = this.userRepository.findByEmail(login).isPresent(); // Is user mail exist
      if (!isLoginExist) { // Otherwise, get user where login is nickname then get mail
        var optionalUser = this.userRepository.findByUsername(login);
        isLoginExist = optionalUser.isPresent();

        if (isLoginExist) {
          User user = optionalUser.get();
          login = user.getEmail();
        }
      }

      String token = "";
      if (isLoginExist) {
        token = this.generateRandomToken();
        Date create_at = new Date();
        long currentTimeMillis = create_at.getTime();
        UserTokenDTO userTokenDTO = new UserTokenDTO(null, login, token, new Date(currentTimeMillis + recovery_validity));
        userTokenService.saveUserToken(userTokenDTO);
        this.passwordRecoveryService.sendEmail(from, login, new EmailForgotPassword(token, frontUrl));
      }
      return ResponseEntity.status(HttpStatus.OK).body("Mail sent");
    } catch (Exception e) {
      e.printStackTrace();

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Server error: " + e.getMessage());
    }
  }

  @PutMapping()
  @SecurityRequirements
  public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest resetPasswordRequest) {
    try {
      // Get login from UserToken
      UserTokenDTO userToken = userTokenService.getUserTokenByToken(resetPasswordRequest.getToken());
      if(userToken == null) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }

      // Get user
      String login = userToken.getUserMail();
      Optional<User> storedUser = this.userRepository.findByEmail(login);

      // Login not existing
      if (!storedUser.isPresent()) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }
      User user = storedUser.get();

      // Token already used or expired
      if (new Date().after(userToken.getExpireAt())) {
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
      }

      // Update password
      String newPassword = resetPasswordRequest.getPassword();
      user.setPassword(newPassword);

      // Verify password
      BindingResult bindingResult = new BeanPropertyBindingResult(user, "user");
      this.springValidator.validate(user, bindingResult);
      if (bindingResult.hasErrors()) {
        throw new RepositoryConstraintViolationException(bindingResult);
      }

      this.publisher.publishEvent(new BeforeSaveEvent(user));
      user = this.userRepository.save(user);
      this.publisher.publishEvent(new AfterSaveEvent(user));

      // Delete userToken
      userTokenService.deleteUserToken(userToken);

      return ResponseEntity.status(HttpStatus.OK).body("Password reset successfully");
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server error");
    }
  }

  /**
   * We generate a random string
   */
  private String generateRandomToken() {
    return UUID.randomUUID().toString();
  }
}
