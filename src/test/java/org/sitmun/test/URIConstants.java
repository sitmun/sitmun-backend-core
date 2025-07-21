package org.sitmun.test;

public class URIConstants {

  public static final String HAL_PROFILE_URI = "http://localhost/api/profile";

  public static final String ACCOUNT_URI = "http://localhost/api/account";

  public static final String APPLICATIONS_URI = "http://localhost/api/applications";

  public static final String APPLICATION_URI = APPLICATIONS_URI + "/{0}";

  public static final String APPLICATION_PARAMETERS_URI_FILTERED = APPLICATION_URI + "/parameters?{1}={2}";

  public static final String APPLICATION_PROJECTION_VIEW = APPLICATION_URI + "?projection=view";

  public static final String APPLICATION_URI_SITUATION_MAP = APPLICATION_URI + "/situationMap";

  public static final String APPLICATION_BACKGROUNDS_URI = "http://localhost/api/application-backgrounds";

  public static final String APPLICATION_BACKGROUND_PROJECTION_VIEW =
    "/api/application-backgrounds/{0}?projection=view";

  public static final String BACKGROUNDS_URI =
    "http://localhost/api/backgrounds";

  public static final String BACKGROUNDS_URI_PROJECTION_VIEW = BACKGROUNDS_URI + "?projection=view";

  public static final String BACKGROUND_URI = BACKGROUNDS_URI + "/{0}";

  public static final String BACKGROUND_URI_CARTOGRAPHY_GROUP = BACKGROUND_URI + "/cartographyGroup";

  public static final String BACKGROUND_URI_CARTOGRAPHY_GROUP_PROJECTION = BACKGROUND_URI_CARTOGRAPHY_GROUP + "?projection=view";

  public static final String CARTOGRAPHIES_URI = "http://localhost/api/cartographies";

  public static final String CARTOGRAPHIES_AVAILABLE_URI = CARTOGRAPHIES_URI + "/search/available?applicationId={0}";

  public static final String CARTOGRAPHY_URI = CARTOGRAPHIES_URI + "/{0}";

  public static final String CARTOGRAPHY_URI_PROJECTION = CARTOGRAPHY_URI + "?projection=view";

  public static final String CARTOGRAPHY_URI_PERMISSION_URI = CARTOGRAPHY_URI + "/permissions";

  public static final String CARTOGRAPHY_URI_PERMISSION_URI_PROJECTION = CARTOGRAPHY_URI_PERMISSION_URI + "?projection=view";

  public static final String CARTOGRAPHY_PERMISSIONS_URI =
    "http://localhost/api/cartography-groups";

  public static final String CARTOGRAPHY_PERMISSION_URI =
    CARTOGRAPHY_PERMISSIONS_URI + "/{0}";

  public static final String CARTOGRAPHY_PERMISSION_ROLES_URI =
    CARTOGRAPHY_PERMISSIONS_URI + "/{0}/roles";

  public static final String CARTOGRAPHY_PERMISSIONS_URI_OR_FILTER =
    CARTOGRAPHY_PERMISSIONS_URI + "?type={0}&type={1}";

  public static final String CARTOGRAPHY_PERMISSIONS_URI_FILTER =
    CARTOGRAPHY_PERMISSIONS_URI + "?type={0}";

  public static final String CARTOGRAPHY_STYLES_URI = "/api/cartography-styles";

  public static final String CARTOGRAPHY_AVAILABILTIY_PROJECTION_VIEW =
    "/api/cartography-availabilities/{0}?projection=view";

  public static final String CARTOGRAPHY_FILTERS_URI = "/api/cartography-filters";

  public static final String CODELIST_VALUES_URI = "http://localhost/api/codelist-values";

  public static final String CODELIST_VALUES_URI_FILTER = CODELIST_VALUES_URI + "?codeListName={0}";

  public static final String CODELIST_VALUE_URI = CODELIST_VALUES_URI + "/{0}";

  public static final String CONFIGURATION_PARAMETERS_URI = "http://localhost/api/configuration-parameters";

  public static final String CONNECTIONS_URI =
    "http://localhost/api/connections";

  public static final String LANGUAGES_URI = "http://localhost/api/languages";

  public static final String SERVICES_URI = "http://localhost/api/services";

  public static final String SERVICE_URI = SERVICES_URI + "/{0}";

  public static final String SERVICE_LAYERS_URI = SERVICE_URI + "/layers";

  public static final String TASKS_URI = "http://localhost/api/tasks";

  public static final String TASKS_URI_PROJECTION_VIEW = TASKS_URI + "?projection=view";

  public static final String TASKS_AVAILABLE_URI = TASKS_URI + "/search/available?applicationId={0}";

  public static final String TASKS_URI_FILTER = TASKS_URI + "?{0}={1}&size=10";

  public static final String TASK_PROJECTION_VIEW = TASKS_URI + "/{0}?projection=view";

  public static final String TASK_AVAILABILITY_PROJECTION_VIEW =
    "/api/task-availabilities/{0}?projection=view";

  public static final String TASK_TYPES_URI = "http://localhost/api/task-types";

  public static final String TERRITORIES_URI = "http://localhost/api/territories";

  public static final String TERRITORY_URI = TERRITORIES_URI + "/{0}";

  public static final String TERRITORY_PROJECTION_VIEW =
    "/api/territories/{0}?projection=view";

  public static final String TERRITORY_GROUP_TYPES_URI = "http://localhost/api/territory-group-types";

  public static final String TERRITORY_TYPES_URI = "http://localhost/api/territory-types";

  public static final String TERRITORY_TYPE_URI = TERRITORY_TYPES_URI + "/{0}";

  public static final String TRANSLATIONS_URI = "http://localhost/api/translations";
  public static final String TRANSLATION_URI = TRANSLATIONS_URI + "/{0}";
  public static final String TRANSLATION_URI_PROJECTION = TRANSLATION_URI + "?projection=view";

  public static final String TREE_URI = "http://localhost/api/trees";

  public static final String TREE_ALL_NODES_URI = "http://localhost/api/trees/{0}/allNodes?projection=view";

  public static final String TREE_NODES_URI = "http://localhost/api/tree-nodes";

  public static final String TREE_NODE_URI = TREE_NODES_URI + "/{0}";

  public static final String TREE_NODE_CARTOGRAPHY_URI = TREE_NODE_URI + "/cartography";

  public static final String TREE_NODE_URI_PROJECTION = TREE_NODE_URI + "?projection=view";

  public static final String TREE_NODE_PARENT_URI = TREE_NODE_URI + "/parent";

  public static final String TREE_NODE_TREE_URI = TREE_NODE_URI + "/tree";

  public static final String TREE_AVAILABLE_APPLICATIONS_URI = "http://localhost/api/trees/{0}/availableApplications";

  public static final String USER_URI = "http://localhost/api/users";

  public static final String USER_CONFIGURATION_PROJECTION_VIEW_PROPERTY_VALUE =
    "/api/user-configurations?projection=view&{0}={1}";

  public static final String USER_CONFIGURATION_PROJECTION_VIEW =
    "/api/user-configurations/{0}?projection=view";

  public static final String USER_POSITION_PROJECTION_VIEW =
    "/api/user-positions/{0}?projection=view";
  public static final String WORKSPACE_URI = "http://localhost/api/workspace";
  public static final String WORKSPACE_APPLICATION_URI = WORKSPACE_URI + "/application/{0}/territory/{1}";
  public static final String TASK_URI = TASKS_URI + "/{0}";
  public static final String TASK_ROLE_URI = TASK_URI + "/roles";
  private static final String ROLES_URI = "http://localhost/api/roles";
  private static final String ROLE_URI = ROLES_URI + "/{0}";
  public static final String ROLE_TASKS_URI = ROLE_URI + "/tasks";
  public static final String ROLE_PERMISSIONS_URI = ROLE_URI + "/permissions";
  public static final String ROLE_APPLICATIONS_URI = ROLE_URI + "/applications";

  public static final String CONFIG_CLIENT_URI = "http://localhost/api/config/client";

  public static final String CONFIG_CLIENT_APPLICATION_URI = CONFIG_CLIENT_URI + "/application";

  public static final String CONFIG_CLIENT_APPLICATION_TERRITORIES_URI = CONFIG_CLIENT_APPLICATION_URI + "/{0}/territories";

  public static final String CONFIG_CLIENT_TERRITORY_URI = CONFIG_CLIENT_URI + "/territory";

  public static final String CONFIG_CLIENT_TERRITORY_APPLICATIONS_URI = CONFIG_CLIENT_TERRITORY_URI + "/{0}/applications";

  public static final String CONFIG_CLIENT_PROFILE_URI = CONFIG_CLIENT_URI + "/profile/{0}/{1}";

  public static final String CONFIG_CLIENT_PROFILE_URI_FILTERED = CONFIG_CLIENT_PROFILE_URI + "?filter={0}";

  public static final String CONFIG_PROXY_URI = "http://localhost/api/config/proxy";

  public static final String CARTOGRAPHY_AVAILABILITY_PROJECTION_VIEW =
    "/api/cartography-availabilities/{0}?projection=view";

  public static final String TASK_PROJECTION_CARTOGRAPHY_VIEW = TASKS_URI + "/{0}/cartography?projection=view";

  public static final String APPLICATION_PARAMETERS_URI = APPLICATION_URI + "/parameters";
}
