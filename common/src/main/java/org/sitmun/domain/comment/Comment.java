package org.sitmun.domain.comment;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.user.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Comment or user's suggestion.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_COMMENT")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Comment {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_COMMENT_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "COM_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_COMMENT_GEN")
  @Column(name = "COM_ID")
  private Integer id;

  /**
   * Coordinate X of the place.
   */
  @Column(name = "COM_COORD_X")
  @NotNull
  private Double coordinateX;

  /**
   * Coordinate Y of the place.
   */
  @Column(name = "COM_COORD_Y")
  @NotNull
  private Double coordinateY;

  /**
   * Name of the person.
   */
  @Column(name = "COM_NAME", length = 250)
  private String name;

  /**
   * Email to receive an answer.
   */
  @Column(name = "COM_EMAIL", length = 250)
  @Email
  private String email;

  /**
   * Title of the suggestion.
   */
  @Column(name = "COM_TITLE", length = 500)
  private String title;

  /**
   * Description.
   */
  @Column(name = "COM_DESC", length = 1000)
  private String description;

  /**
   * Creation date.
   */
  @Column(name = "COM_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * User logged.
   */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "COM_USERID", foreignKey = @ForeignKey(name = "STM_COM_FK_USE"))
  @NotNull
  private User user;

  /**
   * Application used.
   */
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "COM_APPID", foreignKey = @ForeignKey(name = "STM_COM_FK_APP"))
  @NotNull
  private Application application;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Comment))
      return false;

    Comment other = (Comment) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
