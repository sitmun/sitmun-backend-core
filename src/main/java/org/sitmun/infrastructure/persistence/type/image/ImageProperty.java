package org.sitmun.infrastructure.persistence.type.image;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImageProperty {

  private String type;
  private int width;
  private int height;
}
