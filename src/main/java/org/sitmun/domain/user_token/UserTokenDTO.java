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

  private Integer userID;

  private String codeOTP;

  /** Token expiration date */
  private Date expireAt;

  private int attemptCounter;

  private boolean active;
}
