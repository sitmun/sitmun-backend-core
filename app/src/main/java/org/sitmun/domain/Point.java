package org.sitmun.domain;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;

/**
 * Defines a 2D point .
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Point {

  /**
   * The x-value.
   */
  @JsonView({WorkspaceApplication.View.class})
  public Double x;

  /**
   * The y-value.
   */
  @JsonView({WorkspaceApplication.View.class})
  public Double y;
}
