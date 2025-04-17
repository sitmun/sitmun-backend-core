package org.sitmun.domain.user_token;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTokenDTO {
  private Long id;

  /**
   * User mail
   */
  private String userMail;

  /**
   * Token for reset password
   */
  private String tokenId;

  /**
   * Token expiration date
   */
  private Date expireAt;
}
