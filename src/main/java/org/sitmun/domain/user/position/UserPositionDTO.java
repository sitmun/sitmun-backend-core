package org.sitmun.domain.user.position;
import lombok.*;
import java.util.Date;

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