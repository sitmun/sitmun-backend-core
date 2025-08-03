package org.sitmun.domain.territory.type;

import lombok.*;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class TerritoryTypeDTO {
  private Integer id;
  private String name;
  private Boolean official;
  private Boolean topType;
  private Boolean bottomType;
}
