package org.sitmun.domain.cartography;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.util.Date;
import java.util.List;

/**
 * Projections for REST views of a cartography.
 */
@Projection(name = "view", types = Cartography.class)
public interface CartographyProjection {

  @Value("#{target.id}")
  Integer getId();

  /**
   * Cartography name.
   */
  @Value("#{target.name}")
  String getName();

  /**
   * Cartography description.
   */
  @Value("#{target.description}")
  String getDescription();

  /**
   * List of layer identifiers, separated by comas in case of multiple layers.
   */
  @Value("#{target.layers}")
  List<String> getLayers();

  /**
   * Minimum scale visibility.
   */
  @Value("#{target.minimumScale}")
  Integer getMinimumScale();

  /**
   * Maximum visibility.
   */
  @Value("#{target.maximumScale}")
  Integer getMaximumScale();

  /**
   * Cartography order appearance.
   */
  @Value("#{target.order}")
  Integer getOrder();

  /**
   * 0 opaque, 100 translucid.
   */
  @Value("#{target.transparency}")
  Integer getTransparency();

  /**
   * If <code>true</code>, a filter is applied to GetMap requests.
   */
  @Value("#{target.applyFilterToGetMap}")
  Boolean getApplyFilterToGetMap();

  /**
   * If <code>true</code>, the layers are queryable.
   */
  @Value("#{target.queryableFeatureAvailable}")
  Boolean getQueryableFeatureAvailable();

  /**
   * If <code>true</code>, the queryable feature is enabled.
   * This only applies if the layers are queryable
   */
  @Value("#{target.queryableFeatureEnabled}")
  Boolean getQueryableFeatureEnabled();

  /**
   * List of queryable layers.
   */
  @Value("#{target.queryableLayers}")
  List<String> getQueryableLayers();

  /**
   * If <code>true</code>, a filter is applied to GetFeatureInfo requests.
   */
  @Value("#{target.applyFilterToGetFeatureInfo}")
  Boolean getApplyFilterToGetFeatureInfo();

  /**
   * Type.
   */
  @Value("#{target.type}")
  String getType();

  /**
   * If <code>true</code>, the contents of some layers are can be selected.
   */
  @Value("#{target.selectableFeatureEnabled}")
  Boolean getSelectableFeatureEnabled();

  /**
   * Layer available for spatial selection.
   */
  @Value("#{target.selectableLayers}")
  List<String> getSelectableLayers();

  /**
   * If <code>true</code>, ta filter is applied to spatial selection requests.
   */
  @Value("#{target.applyFilterToSpatialSelection}")
  Boolean getApplyFilterToSpatialSelection();

  /**
   * Legend type.
   */
  @Value("#{target.legendType}")
  String getLegendType();

  /**
   * Legend URL.
   */
  @Value("#{target.legendURL}")
  String getLegendURL();

  /**
   * Creation date.
   */
  @Value("#{target.createdDate}")
  Date getCreatedDate();

  /**
   * Direct link to a metadata document.
   */
  @Value("#{target.metadataURL}")
  String getMetadataURL();

  /**
   * Direct link to a dataset file.
   */
  @Value("#{target.datasetURL}")
  String getDatasetURL();

  /**
   * If <code>true</code>, a thematic map can be created from this layer.
   */
  @Value("#{target.thematic}")
  Boolean getThematic();

  /**
   * Geometry type.
   */
  @Value("#{target.geometryType}")
  String getGeometryType();

  /**
   * Grouping source.
   */
  @Value("#{target.source}")
  String getSource();

  /**
   * <code>true</code> if the cartography is blocked and cannot be used.
   */
  @Value("#{target.blocked}")
  Boolean getBlocked();

  /**
   * Portrayal service id.
   */
  @Value("#{target.service?.id}")
  Integer getServiceId();

  /**
   * Portrayal service name.
   */
  @Value("#{target.service?.name}")
  String getServiceName();

  /**
   * Selection service id.
   */
  @Value("#{target.spatialSelectionService?.id}")
  Integer getSpatialSelectionServiceId();

  /**
   * Selection service name.
   */
  @Value("#{target.spatialSelectionService?.name}")
  String getSpatialSelectionServiceName();

  /**
   * <code>true</code> if the cartography must be treated as a set of cartographies, each defined by a style.
   */
  @Value("#{target.useAllStyles}")
  Boolean getUseAllStyles();

  /**
   * Style names.
   */
  @Value("#{target.styles.![name]}")
  List<String> getStylesNames();

}
