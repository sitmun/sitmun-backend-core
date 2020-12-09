package org.sitmun.plugin.core.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("F")
public class BackgroundMap extends CartographyPermission {
}
