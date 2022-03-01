package org.sitmun.domain;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.config.PersistenceConstants;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Geographic Information style.
 */
@Entity
@Table(name = "STM_STY_GI")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartographyStyle {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_STY_GI_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "SGI_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_STY_GI_GEN")
  @Column(name = "SGI_ID")
  private Integer id;

  /**
   * Style name.
   */
  @Column(name = "SGI_NAME", length = PersistenceConstants.IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Style title.
   */
  @Column(name = "SGI_TITLE", length = PersistenceConstants.TITLE)
  private String title;

  /**
   * Style abstract.
   */
  @Column(name = "SGI_ABSTRACT", length = PersistenceConstants.SHORT_DESCRIPTION)
  private String description;

  /**
   * Style legend.
   */
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "width", column = @Column(name = "SGI_LURL_WIDTH")),
    @AttributeOverride(name = "height", column = @Column(name = "SGI_LURL_HEIGHT")),
    @AttributeOverride(name = "format", column = @Column(name = "SGI_LURL_FORMAT")),
    @AttributeOverride(name = "onlineResource", column = @Column(name = "SGI_LURL_URL")),
  })
  private LegendURL legendURL;

  /**
   * This is the preferred style. At most one is preferred per Cartography.
   * <p>
   * If more than one is set as preferred, the behaviour is undefined.
   */
  @Column(name = "SGI_DEFAULT")
  @NotNull
  private Boolean defaultStyle;

  /**
   * Cartography that owns the style.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "SGI_GIID", foreignKey = @ForeignKey(name = "STM_SGI_FK_GEO"))
  @NotNull
  private Cartography cartography;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof CartographyStyle))
      return false;

    CartographyStyle other = (CartographyStyle) o;

    return id != null &&
      id.equals(other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}

