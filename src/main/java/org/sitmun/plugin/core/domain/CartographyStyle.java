package org.sitmun.plugin.core.domain;

import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static org.sitmun.plugin.core.domain.Constants.*;

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
  @Column(name = "SGI_NAME", length = IDENTIFIER)
  @NotBlank
  private String name;

  /**
   * Style title.
   */
  @Column(name = "SGI_TITLE", length = TITLE)
  private String title;

  /**
   * Style abstract.
   */
  @Column(name = "SGI_ABSTRACT", length = SHORT_DESCRIPTION)
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
   * Cartography that owns the style.
   */
  @ManyToOne(fetch = FetchType.LAZY)
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

