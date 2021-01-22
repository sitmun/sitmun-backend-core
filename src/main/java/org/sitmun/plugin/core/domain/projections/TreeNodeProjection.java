package org.sitmun.plugin.core.domain.projections;


import org.sitmun.plugin.core.domain.TreeNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/**
 * Projections for REST views of tree node.
 */
@Projection(name = "view", types = {TreeNode.class})
public interface TreeNodeProjection {

  /**
   * Make accesible the parent node.
   */
  @Value("#{target.parent != null? target.parent.id : null}")
  Integer getParent();

  @Value("#{target.id}")
  Integer getId();

  @Value("#{target.name}")
  String getName();

  @Value("#{target.description}")
  String getDescription();

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
   * If this node is a folder then the cartography must be null.
   */
  @Value("#{target.cartography == null}")
  Boolean getIsFolder();

}
