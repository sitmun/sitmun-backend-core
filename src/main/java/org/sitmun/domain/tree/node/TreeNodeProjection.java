package org.sitmun.domain.tree.node;


import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Projections for REST views of the tree node.
 */
@Projection(name = "view", types = TreeNode.class)
public interface TreeNodeProjection {

  /**
   * Make accesible the parent node.
   */
  @Value("#{target.parent?.id}")
  Integer getParent();

  @Value("#{target.id}")
  Integer getId();

  @Value("#{target.name}")
  String getName();

  @Value("#{target.description}")
  String getDescription();

  @Value("#{target.type}")
  String getNodeType();

  @Value("#{target.tooltip}")
  String getTooltip();

  @Value("#{target.active}")
  Boolean getActive();

  @Value("#{target.radio}")
  Boolean getRadio();

  @Value("#{target.order}")
  Integer getOrder();

  @Value("#{target.metadataURL}")
  String getMetadataURL();

  @Value("#{target.datasetURL}")
  String getDatasetURL();

  @Value("#{target.filterGetMap}")
  Boolean getFilterGetMap();

  @Value("#{target.filterGetFeatureInfo}")
  Boolean getFilterGetFeatureInfo();

  @Value("#{target.queryableActive}")
  Boolean getQueryableActive();

  @Value("#{target.filterSelectable}")
  Boolean getFilterSelectable();

  /**
   * If this node is a folder, then the cartography and task must be null.
   */
  @Value("#{target.cartography == null && target.task == null}")
  Boolean getIsFolder();

  /**
   * Cartography name.
   */
  @Value("#{target.cartography?.name}")
  String getCartographyName();

  /**
   * Cartography identifier.
   */
  @Value("#{target.cartography?.id}")
  Integer getCartographyId();
  
  /**
   * Task name.
   */
  @Value("#{target.task?.name}")
  String getTaskName();

  /**
   * Task identifier.
   */
  @Value("#{target.task?.id}")
  Integer getTaskId();

  @Value("#{target.tree?.id}")
  String getTreeId();

  /**
   * Tree name projection.
   */
  @Value("#{target.tree?.name}")
  String getTreeName();

  /**
   * Tree node style
   */
  @Value("#{target.style}")
  String getStyle();

  /**
   * Tree node image
   */
  @Value("#{target.image}")
  String getImage();

  /**
   * Tree node image name
   */
  @Value("#{target.imageName}")
  String getImageName();

  /**
   * Tree node view mode
   */
  @Value("#{target.viewMode}")
  String getViewMode();

  /**
   * Tree node filterable
   */
  @Value("#{target.filterable}")
  Boolean getFilterable();

  /**
   * Tree node mapping
   */
  @Value("#{target.mapping}")
  Map<String, Object> getMapping();

}
