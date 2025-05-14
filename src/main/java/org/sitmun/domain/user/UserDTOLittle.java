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
public class UserDTOLittle {
  private Integer id;

  /**
   * User mail
   */
  private String email;

  /**
   * User login.
   */
  private String username;

  /**
   * User first name.
   */
  private String firstName;

  /**
   * User last name.
   */
  private String lastName;

  /**
   * Creation date.
   */
  private Date createdDate;
}
