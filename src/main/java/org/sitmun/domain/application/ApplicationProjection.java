package org.sitmun.domain.application;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

/** Projections for REST views of an application. */
@Projection(name = "view", types = Application.class)
public interface ApplicationProjection {

  @Value("#{target.id}")
  Integer getId();

  /** Application name. */
  @Value("#{target.name}")
  String getName();

  /** Application type (external or internal). */
  @Value("#{target.type}")
  String getType();

  /** Title to be shown in the browser and in the application when it is internal. */
  @Value("#{target.title}")
  String getTitle();

  /** CSS to use in this application when it is internal. */
  @Value("#{target.theme}")
  String getTheme();

  /** Scales to be used in this application when it is internal. */
  @Value("#{target.scales}")
  List<String> getScales();

  /** Projection to be used in this application when it is internal. */
  @Value("#{target.srs}")
  String getSrs();

  /**
   * The JSP viewer to be loaded in this application when it is internal or a link to the external
   * application.
   */
  @Value("#{target.jspTemplate}")
  String getJspTemplate();

  /**
   * Wheb tge appliction is internal {@code True} if the application refreshes automatically; {@code
   * False} if an "update map" button is required.
   */
  @Value("#{target.treeAutoRefresh}")
  Boolean getTreeAutoRefresh();

  /** Can access a "parent" territory. */
  @Value("#{target.accessParentTerritory}")
  Boolean getAccessParentTerritory();

  /** Can access a "children" territory. */
  @Value("#{target.accessChildrenTerritory}")
  Boolean getAccessChildrenTerritory();

  /** Situation map when the application is internal. */
  @Value("#{target.situationMap?.id}")
  Integer getSituationMapId();

  /** Created date. */
  @Value("#{target.createdDate}")
  Date getCreatedDate();

  /** Application logo. */
  @Value("#{target.logo}")
  String getLogo();

  /** Application description. */
  @Value("#{target.description}")
  String getDescription();

  /** Information of maintenance */
  @Value("#{target.maintenanceInformation}")
  String getMaintenanceInformation();

  /** Is the app usable */
  @Value("#{target.isUnavailable}")
  Boolean getIsUnavailable();

  /** Last update date */
  @Value("#{target.lastUpdate}")
  Date getLastUpdate();

  /** Creator ID of the app */
  @Value("#{target.creator?.id}")
  Integer getCreatorId();
}
