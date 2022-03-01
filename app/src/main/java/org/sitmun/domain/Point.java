package org.sitmun.domain;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.views.Views;

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
  @JsonView({Views.WorkspaceApplication.class})
  public Double x;

  /**
   * The y-value.
   */
  @JsonView({Views.WorkspaceApplication.class})
  public Double y;
}
