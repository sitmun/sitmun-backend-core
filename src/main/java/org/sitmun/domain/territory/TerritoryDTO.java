package org.sitmun.domain.territory;
import lombok.*;
import org.sitmun.domain.territory.type.TerritoryTypeDTO;
import org.sitmun.domain.user.position.UserPositionDTO;
import org.sitmun.infrastructure.persistence.type.envelope.EnvelopeDTO;
import org.sitmun.infrastructure.persistence.type.point.PointDTO;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TerritoryDTO {
  private Integer id;
  private String code;
  private String name;
  private String territorialAuthorityName;
  private String territorialAuthorityAddress;
  private String territorialAuthorityEmail;
  private String territorialAuthorityLogo;
  private String scope;
  private EnvelopeDTO extent;
  private PointDTO center;
  private Integer defaultZoomLevel;
  private Boolean blocked;
  private TerritoryTypeDTO type;
  private String note;
  private String legal;
  private Date createdDate;
  @Builder.Default
  private Set<UserPositionDTO> positions = new HashSet<>();
}