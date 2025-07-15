package org.sitmun.domain.user_token;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

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
  private Long id;

  @Column(name = "USER_MAIL", nullable = false)
  private String userMail;

  @Column(name = "TOKEN_ID", nullable = false)
  private String tokenId;

  @Column(name = "EXPIRE_AT", updatable = false)
  private Date expireAt;
}
