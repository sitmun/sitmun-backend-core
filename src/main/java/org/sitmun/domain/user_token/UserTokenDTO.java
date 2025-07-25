package org.sitmun.domain.user_token;

import java.util.Date;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTokenDTO {
  private Integer id;

  /** User mail */
  private String userMail;

  /** Token for reset password */
  private String tokenId;

  /** Token expiration date */
  private Date expireAt;
}
