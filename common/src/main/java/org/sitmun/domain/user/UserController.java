package org.sitmun.domain.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    springValidator = new SpringValidatorAdapter(validator);
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
      User mergedUser = getUser(updatedUser, user);
      BindingResult bindingResult = new BeanPropertyBindingResult(mergedUser, "user");
      springValidator.validate(mergedUser, bindingResult);
      if (bindingResult.hasErrors()) {
        throw new RepositoryConstraintViolationException(bindingResult);
      }
      publisher.publishEvent(new BeforeSaveEvent(mergedUser));
      mergedUser = userRepository.save(mergedUser);
      publisher.publishEvent(new AfterSaveEvent(mergedUser));
      return ResponseEntity.ok(userToDto(mergedUser));
    }
      return ResponseEntity.notFound().build();
  }

  private static User getUser(UserDTO updatedUser, User user) {
    user.setUsername(updatedUser.getUsername());
    user.setPassword(updatedUser.getPassword());
    user.setFirstName(updatedUser.getFirstName());
    user.setLastName(updatedUser.getLastName());
    user.setIdentificationNumber(updatedUser.getIdentificationNumber());
    user.setIdentificationType(updatedUser.getIdentificationType());
    user.setAdministrator(updatedUser.getAdministrator());
    user.setBlocked(updatedUser.getBlocked());
    user.setGeneric(updatedUser.getGeneric());
    return user;
  }

  /**
   * Get accounts.
   */
  @GetMapping
  @ResponseBody
  public ResponseEntity<UserDTO> getAccount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Optional<UserDTO> storedUser = userRepository.findByUsername(authentication.getName()).map(UserController::userToDto);
    return storedUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  private static UserDTO userToDto(User user) {
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
