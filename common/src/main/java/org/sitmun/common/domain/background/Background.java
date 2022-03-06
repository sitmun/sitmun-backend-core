package org.sitmun.common.domain.background;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.common.domain.application.background.ApplicationBackground;
import org.sitmun.common.domain.cartography.permission.CartographyPermission;
import org.sitmun.common.types.http.Http;
import org.sitmun.common.views.Views;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.sitmun.common.config.PersistenceConstants.*;

/**
 * Background.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_BACKGRD")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Background {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_BACKGRD_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "BAC_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_BACKGRD_GEN")
  @Column(name = "BAC_ID")
  private Integer id;

  /**
   * Name.
   */
  @Column(name = "BAC_NAME", length = IDENTIFIER)
  @NotBlank
  @JsonView({Views.WorkspaceApplication.class})
  private String name;

  /**
   * Representative image or icon.
   */
  @Column(name = "BAC_IMAGE", length = URL)
  @Http
  @JsonView({Views.WorkspaceApplication.class})
  private String image;

  /**
   * Description.
   */
  @Column(name = "BAC_DESC", length = SHORT_DESCRIPTION)
  private String description;

  /**
   * True if it should be considered active by default in applications.
   */
  @Column(name = "BAC_ACTIVE")
  private Boolean active;

  /**
   * Created date.
   */
  @Column(name = "BAC_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * Cartography group used as background.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "BAC_GGIID", foreignKey = @ForeignKey(name = "STM_BAC_FK_GGI"))
  @JsonView({Views.WorkspaceApplication.class})
  private CartographyPermission cartographyGroup;

  /**
   * Applications where it is used.
   */
  @OneToMany(mappedBy = "background", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ApplicationBackground> applications = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof Background))
      return false;

    Background other = (Background) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
