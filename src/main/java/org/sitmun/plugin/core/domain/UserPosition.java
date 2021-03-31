package org.sitmun.plugin.core.domain;


import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.plugin.core.constraints.CodeList;
import org.sitmun.plugin.core.constraints.CodeLists;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Date;

import static org.sitmun.plugin.core.domain.Constants.IDENTIFIER;

/**
 * User position in a territory.
 */
@Entity
@Table(name = "STM_POST", uniqueConstraints = {
  @UniqueConstraint(columnNames = {"POS_USERID", "POS_TERID"})})
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserPosition {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_POST_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "POS_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_POST_GEN")
  @Column(name = "POS_ID")
  private Integer id;

  /**
   * Position description.
   */
  @Column(name = "POS_POST", length = 250)
  private String name;

  /**
   * Organization.
   */
  @Column(name = "POS_ORG", length = 250)
  private String organization;

  /**
   * Email.
   */
  @Column(name = "POS_EMAIL", length = 250)
  @Email
  private String email;

  /**
   * Creation date.
   */
  @Column(name = "POS_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  private Date createdDate;

  /**
   * Expiration date.
   */
  @Column(name = "POS_EXPIRATION")
  @Temporal(TemporalType.TIMESTAMP)
  private Date expirationDate;

  /**
   * Type of user (only used in some cases).
   */
  @Column(name = "POS_TYPE", length = IDENTIFIER)
  @CodeList(CodeLists.USER_POSITION_TYPE)
  private String type;

  /**
   * User.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "POS_USERID", foreignKey = @ForeignKey(name = "STM_POS_FK_USE"))
  @NotNull
  private User user;

  /**
   * Territory.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "POS_TERID", foreignKey = @ForeignKey(name = "STM_POS_FK_TER"))
  @NotNull
  private Territory territory;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof UserPosition))
      return false;

    UserPosition other = (UserPosition) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
