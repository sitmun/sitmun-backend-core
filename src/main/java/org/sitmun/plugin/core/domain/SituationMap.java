package org.sitmun.plugin.core.domain;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("M")
public class SituationMap extends CartographyPermission {
}
