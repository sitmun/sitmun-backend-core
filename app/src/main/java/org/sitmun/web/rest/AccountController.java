package org.sitmun.web.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.sitmun.common.domain.user.User;
import org.sitmun.common.domain.user.UserRepository;
import org.sitmun.common.security.SecurityUtils;
import org.sitmun.web.rest.dto.UserDTO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.data.rest.webmvc.support.RepositoryConstraintViolationExceptionMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.*;

import javax.validation.Validator;
import java.util.Optional;

@RestController
@RequestMapping("/api/account")
@Tag(name = "account", description = "user account management")
public class AccountController {

  private final SpringValidatorAdapter springValidator;

  private final UserRepository userRepository;

  private final MessageSourceAccessor messageSourceAccessor;

  private final ApplicationEventPublisher publisher;

  /**
   * Constructor.
   */
  public AccountController(MessageSource messageSource, Validator validator, UserRepository userRepository, ApplicationEventPublisher publisher) {
    Assert.notNull(messageSource, "MessageSource must not be null!");
    this.messageSourceAccessor = new MessageSourceAccessor(messageSource);
    this.springValidator = new SpringValidatorAdapter(validator);
    this.userRepository = userRepository;
    this.publisher = publisher;
  }

  /**
   * Update en existing account.
   *
   * @param updatedUser account to be updated
   * @return ok if the account has been updated
   */
  @PutMapping
  @ResponseBody
  public ResponseEntity<UserDTO> saveAccount(@RequestBody UserDTO updatedUser) {
    Optional<String> optLogin = SecurityUtils.getCurrentUserLogin();
    if (optLogin.isPresent()) {
      Optional<User> storedUser = userRepository.findByUsername(optLogin.get());
      if (storedUser.isPresent()) {
        User user = storedUser.get();
        user.setUsername(updatedUser.getUsername());
        user.setPassword(updatedUser.getPassword());
        user.setFirstName(updatedUser.getFirstName());
        user.setLastName(updatedUser.getLastName());
        user.setIdentificationNumber(user.getIdentificationNumber());
        user.setIdentificationType(user.getIdentificationType());
        user.setAdministrator(user.getAdministrator());
        user.setBlocked(user.getBlocked());
        user.setGeneric(user.getGeneric());
        BindingResult bindingResult = new BeanPropertyBindingResult(user, "user");
        springValidator.validate(user, bindingResult);
        if (bindingResult.hasErrors()) {
          throw new RepositoryConstraintViolationException(bindingResult);
        }
        publisher.publishEvent(new BeforeSaveEvent(user));
        user = userRepository.save(user);
        publisher.publishEvent(new AfterSaveEvent(user));
        return ResponseEntity.ok(userToDto(user));
      } else {
        return ResponseEntity.notFound().build();
      }
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @ExceptionHandler(RepositoryConstraintViolationException.class)
  ResponseEntity<RepositoryConstraintViolationExceptionMessage> handleRepositoryConstraintViolationException(
    RepositoryConstraintViolationException o_O) {
    return new ResponseEntity<>(new RepositoryConstraintViolationExceptionMessage(o_O, messageSourceAccessor),
      new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  /**
   * Get accounts.
   */
  @GetMapping
  @ResponseBody
  @Transactional(readOnly = true)
  public ResponseEntity<UserDTO> getAccount() {
    Optional<String> optLogin = SecurityUtils.getCurrentUserLogin();
    if (optLogin.isPresent()) {
      Optional<UserDTO> storedUser = userRepository.findByUsername(optLogin.get()).map(this::userToDto);
      return storedUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  private UserDTO userToDto(User user) {
    return UserDTO.builder()
      .id(user.getId())
      .username(user.getUsername())
      .passwordSet(user.getPassword() != null && !user.getPassword().isEmpty())
      .firstName(user.getFirstName())
      .lastName(user.getLastName())
      .identificationNumber(user.getIdentificationNumber())
      .identificationType(user.getIdentificationType())
      .administrator(user.getAdministrator())
      .blocked(user.getBlocked())
      .generic(user.getGeneric())
      .build();
  }
}
