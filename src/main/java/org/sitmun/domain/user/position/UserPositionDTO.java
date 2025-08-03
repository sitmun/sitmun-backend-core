package org.sitmun.domain.user.position;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPositionDTO {
  private Integer id;
  private String name;
  private String organization;
  private String email;
  private Date createdDate;
  private Date lastModifiedDate;
  private Date expirationDate;
  private String type;
  private Integer userId;
  private Integer territoryId;
}
