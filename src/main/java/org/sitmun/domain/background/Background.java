package org.sitmun.domain.background;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.authorization.dto.ClientConfigurationViews;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.application.background.ApplicationBackground;
import org.sitmun.domain.cartography.permission.CartographyPermission;
import org.sitmun.infrastructure.persistence.type.basic.Http;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

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
  @Column(name = "BAC_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String name;

  /**
   * Representative image or icon.
   */
  @Column(name = "BAC_IMAGE", length = PersistenceConstants.URL)
  @Http
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private String image;

  /**
   * Description.
   */
  @Column(name = "BAC_DESC", length = PersistenceConstants.SHORT_DESCRIPTION)
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
  @ManyToOne
  @JoinColumn(name = "BAC_GGIID", foreignKey = @ForeignKey(name = "STM_BAC_FK_GGI"))
  @JsonView(ClientConfigurationViews.ApplicationTerritory.class)
  private CartographyPermission cartographyGroup;

  /**
   * Applications where it is used.
   */
  @OneToMany(mappedBy = "background", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ApplicationBackground> applications = new HashSet<>();

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }

    if (!(obj instanceof Background)) {
        return false;
    }

    Background other = (Background) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
