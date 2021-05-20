package org.sitmun.plugin.core.domain.projections;

import org.sitmun.plugin.core.domain.CartographyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.List;

/**
 * Projections for REST views of an application.
 */
@Projection(name = "view", types = {CartographyFilter.class})
public interface CartographyFilterProjection {

  @Value("#{target.id}")
  Integer getId();

  /**
   * Filter name.
   */
  @Value("#{target.name}")
  String getName();

  /**
   * If <code>true</code>, this filter is required.
   */
  @Value("#{target.required}")
  Boolean getRequired();

  /**
   * Type of filter: custom or required.
   */
  @Value("#{target.type}")
  String getType();

  /**
   * Territorial level id.
   */
  @Value("#{target.territorialLevel?.id}")
  Integer getTerrorialLevelId();

  /**
   * Territorial level name.
   */
  @Value("#{target.territorialLevel?.name}")
  String getTerrorialLevelName();

  /**
   * Column where the filter applies.
   */
  @Value("#{target.column}")
  String getColumn();

  /**
   * A row is part of the filter if the value of the column is one of these values.
   */
  @Value("#{target.values}")
  List<String> getValues();

  /**
   * Type of filter value.
   */
  @Value("#{target.valueType}")
  String getValueType();
}
