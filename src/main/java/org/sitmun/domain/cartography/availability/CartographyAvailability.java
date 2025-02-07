package org.sitmun.domain.cartography.availability;

import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.sitmun.domain.PersistenceConstants;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.territory.Territory;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Objects;

/**
 * Grants availability of a Geographic Information in a Territory.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "STM_AVAIL_GI", uniqueConstraints = @UniqueConstraint(name = "STM_DCA_UK", columnNames = {"AGI_TERID", "AGI_GIID"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CartographyAvailability {

  /**
   * Unique identifier.
   */
  @TableGenerator(
    name = "STM_AVAIL_GI_GEN",
    table = "STM_SEQUENCE",
    pkColumnName = "SEQ_NAME",
    valueColumnName = "SEQ_COUNT",
    pkColumnValue = "AGI_ID",
    allocationSize = 1)
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "STM_AVAIL_GI_GEN")
  @Column(name = "AGI_ID")
  private Integer id;

  /**
   * Creation date.
   */
  @Column(name = "AGI_CREATED")
  @Temporal(TemporalType.TIMESTAMP)
  @CreatedDate
  private Date createdDate;

  /**
   * Owner of the Geographic Information.
   * Keeps the owner's name when ownership is not obvious or is an exception.
   */
  @Column(name = "AGI_OWNER", length = PersistenceConstants.IDENTIFIER)
  private String owner;

  /**
   * Territory allowed to access to the cartography.
   */
  @ManyToOne
  @JoinColumn(name = "AGI_TERID", foreignKey = @ForeignKey(name = "STM_AGI_FK_TER"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Territory territory;

  /**
   * Cartography allowed to the territory.
   */
  @ManyToOne
  @JoinColumn(name = "AGI_GIID", foreignKey = @ForeignKey(name = "STM_AGI_FK_GEO"))
  @OnDelete(action = OnDeleteAction.CASCADE)
  @NotNull
  private Cartography cartography;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CartographyAvailability)) {
      return false;
    }

    CartographyAvailability other = (CartographyAvailability) obj;

    return Objects.equals(id, other.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
