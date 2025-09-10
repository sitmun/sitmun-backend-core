package org.sitmun.domain.language;

import lombok.*;

/** User DTO. */
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LanguageDTO {
  private int id;
  private String name;
  private String shortName;
}
