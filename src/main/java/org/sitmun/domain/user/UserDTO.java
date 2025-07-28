package org.sitmun.domain.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Date;
import lombok.*;

/** User DTO. */
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDTO {

  private Integer id;

  /** User mail */
  @Email(message = "Email should be valid")
  private String email;

  /** User login. */
  @NotBlank(message = "Username is required")
  @Size(min = 1, max = 50, message = "Username must be between 1 and 50 characters")
  private String username;

  /** User password hash. */
  private String password;

  /** User password hash. */
  private Boolean passwordSet;

  /** User's first name. */
  @Size(max = 100, message = "First name must not exceed 100 characters")
  private String firstName;

  /** User last name. */
  @Size(max = 100, message = "Last name must not exceed 100 characters")
  private String lastName;

  /** User identification number. */
  @Size(max = 50, message = "Identification number must not exceed 50 characters")
  private String identificationNumber;

  /** User identification type. */
  @Size(max = 20, message = "Identification type must not exceed 20 characters")
  private String identificationType;

  /** If <code>true</code>, the user is a system administrator. */
  private Boolean administrator;

  /** If <code>true</code>, the user is blocked and cannot log to the system. */
  private Boolean blocked;

  /** Creation date. */
  private Date createdDate;
}
