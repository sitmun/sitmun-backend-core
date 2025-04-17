package org.sitmun.domain.user_token;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
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
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "USER_TOKEN_ID")
  private Long id;

  @Column(name = "USER_MAIL", nullable = false)
  private String userMail;

  @Column(name = "TOKEN_ID", nullable = false)
  private String tokenId;

  @Column(name = "EXPIRE_AT", updatable = false)
  private Date expireAt;
}
