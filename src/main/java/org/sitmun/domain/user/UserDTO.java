package org.sitmun.domain.user;

import lombok.*;

import java.util.Date;

/**
 * User DTO.
 */
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDTO {

  private Integer id;

  /**
   * User login.
   */
  private String username;

  /**
   * User password hash.
   */
  private String password;

  /**
   * User password hash.
   */
  private Boolean passwordSet;

  /**
   * User first name.
   */
  private String firstName;

  /**
   * User last name.
   */
  private String lastName;

  /**
   * User identification number.
   */
  private String identificationNumber;

  /**
   * User identification type.
   */
  private String identificationType;

  /**
   * If <code>true</code>, the user is a system administrator.
   */
  private Boolean administrator;

  /**
   * If <code>true</code>, the user is blocked and cannot log to the system.
   */
  private Boolean blocked;

  /**
   * Creation date.
   */
  private Date createdDate;
}
