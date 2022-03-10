package org.sitmun.common.domain.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.*;

import javax.validation.Validator;
import java.util.Optional;

@RestController
@RequestMapping("/api/account")
@Tag(name = "account", description = "user account management")
public class UserController {

  private final SpringValidatorAdapter springValidator;

  private final UserRepository userRepository;

  private final ApplicationEventPublisher publisher;

  /**
   * Constructor.
   */
  public UserController(Validator validator, UserRepository userRepository, ApplicationEventPublisher publisher) {
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
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Optional<User> storedUser = userRepository.findByUsername(authentication.getName());
    if (storedUser.isPresent()) {
      User user = storedUser.get();
      user.setUsername(updatedUser.getUsername());
      user.setPassword(updatedUser.getPassword());
      user.setFirstName(updatedUser.getFirstName());
      user.setLastName(updatedUser.getLastName());
      user.setIdentificationNumber(updatedUser.getIdentificationNumber());
      user.setIdentificationType(updatedUser.getIdentificationType());
      user.setAdministrator(updatedUser.getAdministrator());
      user.setBlocked(updatedUser.getBlocked());
      user.setGeneric(updatedUser.getGeneric());
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
  }

  /**
   * Get accounts.
   */
  @GetMapping
  @ResponseBody
  @Transactional(readOnly = true)
  public ResponseEntity<UserDTO> getAccount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Optional<UserDTO> storedUser = userRepository.findByUsername(authentication.getName()).map(this::userToDto);
    return storedUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
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
