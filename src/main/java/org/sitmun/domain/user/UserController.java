package org.sitmun.domain.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.event.AfterSaveEvent;
import org.springframework.data.rest.core.event.BeforeSaveEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@Tag(name = "account", description = "user account management")
@Validated
public class UserController {

  private final SpringValidatorAdapter springValidator;

  private final UserRepository userRepository;
  private final ApplicationEventPublisher publisher;

  /** Constructor. */
  public UserController(
      Validator validator, UserRepository userRepository, ApplicationEventPublisher publisher) {
    springValidator = new SpringValidatorAdapter(validator);
    this.userRepository = userRepository;
    this.publisher = publisher;
  }

  private static User getUser(UserDTO updatedUser, User user) {
    user.setUsername(updatedUser.getUsername());
    user.setPassword(updatedUser.getPassword());
    user.setFirstName(updatedUser.getFirstName());
    user.setLastName(updatedUser.getLastName());
    user.setIdentificationNumber(updatedUser.getIdentificationNumber());
    user.setIdentificationType(updatedUser.getIdentificationType());
    user.setEmail(updatedUser.getEmail());
    return user;
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
        .email(user.getEmail())
        .createdDate(user.getCreatedDate())
        .build();
  }

  private static UserDTOLittle userToDtoLittle(User user) {
    return UserDTOLittle.builder()
        .id(user.getId())
        .username(user.getUsername())
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .email(user.getEmail())
        .createdDate(user.getCreatedDate())
        .build();
  }

  /**
   * Update an existing account.
   *
   * @param updatedUser account to be updated
   * @return ok if the account has been updated
   */
  @PutMapping
  public ResponseEntity<UserDTO> saveAccount(@Valid @RequestBody UserDTO updatedUser) {
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

  /** Get accounts. */
  @GetMapping
  public ResponseEntity<UserDTO> getAccount() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Optional<UserDTO> storedUser =
        userRepository.findByUsername(authentication.getName()).map(UserController::userToDto);
    return storedUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDTO> getAccountById(@PathVariable Integer id) {
    Optional<UserDTO> storedUser = userRepository.findById(id).map(UserController::userToDto);
    return storedUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/public/{id}")
  public ResponseEntity<UserDTOLittle> getAccountByIdPublic(@PathVariable Integer id) {
    Optional<UserDTOLittle> storedUser =
        userRepository.findById(id).map(UserController::userToDtoLittle);
    return storedUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  /** Get all accounts */
  @GetMapping("/all")
  public ResponseEntity<List<UserDTO>> getAllAccounts() {
    List<User> users = userRepository.findAll().stream().toList();
    if (users.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    List<UserDTO> userDTOs =
        users.stream().map(UserController::userToDto).collect(Collectors.toList());
    return ResponseEntity.ok(userDTOs);
  }
}
