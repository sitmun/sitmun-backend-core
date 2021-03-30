package org.sitmun.plugin.core.domain;


import lombok.*;
import org.sitmun.plugin.core.constraints.HttpURL;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.sitmun.plugin.core.domain.Constants.*;

/**
 * Background.
 */
@Entity
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
  private String name;

  /**
   * Representative image or icon.
   */
  @Column(name = "BAC_IMAGE", length = URL)
  @HttpURL
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
  private Date createdDate;

  /**
   * Cartography group used as background.
   */
  @ManyToOne
  @JoinColumn(name = "BAC_GGIID", foreignKey = @ForeignKey(name = "STM_BAC_FK_GGI"))
  private CartographyPermission cartographyGroup;

  /**
   * Backgrounds maps.
   */
  @OneToMany(mappedBy = "background", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<ApplicationBackground> backgrounds = new HashSet<>();

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
