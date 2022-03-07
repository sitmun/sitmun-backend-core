package org.sitmun.common.types.point;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.sitmun.feature.client.config.Views;

/**
 * Defines a 2D point .
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
@JsonView({Views.WorkspaceApplication.class})
public class Point {

  /**
   * The x-value.
   */
  public Double x;

  /**
   * The y-value.
   */
  public Double y;
}
