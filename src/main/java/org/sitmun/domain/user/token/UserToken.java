package org.sitmun.domain.user.token;

import jakarta.persistence.*;
import java.util.Date;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_TOKEN_USER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UserToken {
  @TableGenerator(
      name = "STM_USER_TOKEN_GEN",
      table = "STM_SEQUENCE",
      pkColumnName = "SEQ_NAME",
      valueColumnName = "SEQ_COUNT",
      pkColumnValue = "USER_TOKEN_ID",
      allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_USER_TOKEN_GEN")
  @Column(name = "USER_TOKEN_ID")
  private Integer id;

  @Column(name = "USER_ID", nullable = false)
  private Integer userID;

  @Column(name = "CODE_OTP", nullable = false)
  private String codeOTP;

  @Column(name = "EXPIRE_AT", nullable = false)
  private Date expireAt;

  @Column(name = "ATTEMPT_COUNTER", nullable = false)
  private int attemptCounter;

  @Column(name = "ACTIVE", nullable = true)
  private boolean active;
}
