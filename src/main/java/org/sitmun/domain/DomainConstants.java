package org.sitmun.domain;

import java.util.Map;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.tree.Tree;

/** Domain constants for SITMUN entities and properties. */
public class DomainConstants {

  private DomainConstants() {
    // Utility class
  }

  /** Task-related constants. */
  public static class Tasks {
    // Property keys
    public static final String PROPERTY_PARAMETERS = "parameters";
    public static final String PROPERTY_COMMAND = "command";
    public static final String PROPERTY_FIELDS = "fields";
    public static final String PROPERTY_SCOPE = "scope";
    public static final String PROPERTY_TEMPLATE_HTML = "templateHtml";
    public static final String PROPERTY_TEMPLATE_EDITOR_STATE = "templateEditorState";
    public static final String PROPERTY_EXPORT_ENGINE = "exportEngine";
    public static final String PROPERTY_FORMAT = "format";
    public static final String PROPERTY_WIDTH = "width";
    public static final String PROPERTY_HEIGHT = "height";
    public static final String PROPERTY_SRS = "srs";
    public static final String PROPERTY_BBOX_MARGIN_PERCENT = "bboxMarginPercent";
    public static final String PROPERTY_MAP_SOURCES = "mapSources";

    /**
     * Task property {@code downloadFormat}: output format for the download button shown in the MIA
     * popup when this task is a template child. Supported values: {@code "pdf"}, {@code "xml"}.
     * If absent or blank, no download button is shown.
     */
    public static final String PROPERTY_DOWNLOAD_FORMAT = "downloadFormat";

    /**
     * Task property {@code pageSize}: PDF page size used by document export tasks. Supported
     * values: {@code "A4"}, {@code "A3"}.
     */
    public static final String PROPERTY_PAGE_SIZE = "pageSize";

    /**
     * Task property {@code pageOrientation}: PDF page orientation used by document export tasks.
     * Supported values: {@code "portrait"}, {@code "landscape"}.
     */
    public static final String PROPERTY_PAGE_ORIENTATION = "pageOrientation";

    /**
     * Task property {@code downloadSource}: path to a Jasper report file (.jrxml / .xml) relative
     * to the configured {@code sitmun.template.export.allowed-file-path-prefix} directory.
     * Only relevant when {@code downloadFormat} is {@code "xml"}.
     */
    public static final String PROPERTY_DOWNLOAD_SOURCE = "downloadSource";

    // Parameter properties
    public static final String PARAMETERS_NAME = "name";
    public static final String PARAMETERS_VARIABLE = "variable";
    public static final String PARAMETERS_FIELD = "field";
    public static final String PARAMETERS_VALUE = "value";
    public static final String PARAMETERS_LABEL = "label";
    public static final String PARAMETERS_TYPE = "type";
    public static final String PARAMETERS_REQUIRED = "required";
    public static final String PARAMETERS_DESCRIPTION = "description";

    /**
     * Flag indicating a parameter is provided by the backend and should not be exposed to clients.
     * Backend-only secrets (e.g., API tokens, database credentials).
     */
    public static final String PARAMETERS_PROVIDED = "provided";

    // Field properties
    public static final String FIELDS_NAME = "name";
    public static final String FIELDS_TYPE = "type";
    public static final String FIELDS_LABEL = "label";
    public static final String FIELDS_EDITABLE = "editable";
    public static final String FIELDS_REQUIRED = "required";
    public static final String FIELDS_SELECTABLE = "selectable";
    public static final String FIELDS_LIST_VALUES = "listValues";
    public static final String FIELDS_QUERY = "query";

    // Field type values
    public static final String FIELD_TYPE_TEXT = "text";

    // Scope types (more-info tasks)
    public static final String SCOPE_URL = "URL";
    public static final String SCOPE_API = "API";
    public static final String SCOPE_SQL = "SQL";
    public static final String SCOPE_RESOURCE = "RESOURCE";

    // Scope types (query tasks)
    public static final String SCOPE_CARTOGRAPHY_QUERY = "cartography-query";
    public static final String SCOPE_SQL_QUERY = "sql-query";
    public static final String SCOPE_WEB_API_QUERY = "web-api-query";
    public static final String SCOPE_WEB_API_QUERY_NO_PROXY = "web-api-query-no-proxy";
    public static final String SCOPE_URL_QUERY = "external-link";
    public static final String SCOPE_RESOURCE_QUERY = "resource-query";

    // Task relation types
    public static final String RELATION_TYPE_QUERY_TASK = "query-task";
    public static final String RELATION_TYPE_TEMPLATE_TASK = "template-task";
    public static final String RELATION_TYPE_TEMPLATE_NESTED = "template-nested";

    /** Client profile task id prefix used in REST profile payloads (e.g. {@code task/42}). */
    public static final String TASK_PROFILE_ID_PREFIX = "task/";

    /**
     * Client profile cartography id prefix; layer entries use {@code layer/<id>} (see {@code
     * ProfileMapper}).
     */
    public static final String PROFILE_LAYER_ID_PREFIX = "layer/";

    /** Client profile cartography-group id prefix in REST payloads ({@code group/<id>}). */
    public static final String PROFILE_GROUP_ID_PREFIX = "group/";

    /** Client profile service id prefix ({@code service/<id>}). */
    public static final String PROFILE_SERVICE_ID_PREFIX = "service/";

    /** Client profile tree id prefix ({@code tree/<id>}). */
    public static final String PROFILE_TREE_ID_PREFIX = "tree/";

    /** Client profile tree-node id prefix ({@code node/<id>}). */
    public static final String PROFILE_NODE_ID_PREFIX = "node/";

    // Resource properties
    public static final String PROPERTY_MIME_TYPE = "mimeType";
    public static final String PROPERTY_FILENAME = "filename";

    // HTTP API task properties
    public static final String PROPERTY_BODY = "body";
    public static final String PROPERTY_AUTHENTICATION_MODE = "authenticationMode";
    public static final String PROPERTY_USER = "user";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_HEADERS = "headers";
    public static final String PROPERTY_QUERY_PARAMS = "queryParams";

    /** Task property {@code authenticationMode}: no proxy auth (matches admin codelist). */
    public static final String AUTHENTICATION_MODE_NONE = "None";

    // Parameter types
    public static final String TYPE_STRING = "string";
    public static final String TYPE_NUMBER = "number";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_ARRAY = "array";
    public static final String TYPE_OBJECT = "object";
    public static final String TYPE_NULL = "null";

    // Parameter value types (for task parameters)
    public static final String PARAM_TYPE_QUERY = "query";
    public static final String PARAM_TYPE_TEMPLATE = "template";
    public static final String PARAM_TYPE_BODY = "body";

    // Task type IDs (from STM_TSK_TYP)
    public static final int TASK_TYPE_ID_EDIT = 0;
    public static final int TASK_TYPE_ID_BASIC = 1;
    public static final int TASK_TYPE_ID_LOCATOR = 4;
    public static final int TASK_TYPE_ID_QUERY = 5;
    public static final int TASK_TYPE_ID_MORE_INFO = 6;
    public static final int TASK_TYPE_ID_MAP_IMAGE = 18;
    public static final int TASK_TYPE_ID_DOCUMENT_EXPORT = 17;
    public static final int TASK_TYPE_ID_TEMPLATE = 15;
    public static final int TASK_TYPE_ID_MORE_INFO_ADVANCED = 16;

    private static Integer taskTypeId(Task task) {
      if (task == null || task.getType() == null) {
        return null;
      }
      return task.getType().getId();
    }

    public static boolean isBasicTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_BASIC).equals(taskTypeId(task));
    }

    /**
     * Basic task with legacy JSON: no {@link #PROPERTY_SCOPE} (three-field parameters only; see
     * {@code TaskBasicValidator}).
     */
    public static boolean isLegacyBasicTask(Task task) {
      if (!isBasicTask(task)) {
        return false;
      }
      Map<String, Object> properties = task.getProperties();
      return properties == null || !properties.containsKey(PROPERTY_SCOPE);
    }

    public static boolean isLocatorTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_LOCATOR).equals(taskTypeId(task));
    }

    public static boolean isMoreInfoTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_MORE_INFO).equals(taskTypeId(task));
    }

    public static boolean isDocumentExportTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_DOCUMENT_EXPORT).equals(taskTypeId(task));
    }

    public static boolean isMoreInfoAdvancedTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_MORE_INFO_ADVANCED).equals(taskTypeId(task));
    }

    public static boolean isMapImageTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_MAP_IMAGE).equals(taskTypeId(task));
    }

    /** Any task whose type id is {@link #TASK_TYPE_ID_QUERY} (scope/FK rules use other helpers). */
    public static boolean isQueryTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_QUERY).equals(taskTypeId(task));
    }

    public static boolean isSqlQueryTask(Task task) {
      if (!isQueryTask(task)) {
        return false;
      }
      if (task.getConnection() == null) {
        return false;
      }
      // When scope is explicitly set, it must match sql-query
      Map<String, Object> properties = task.getProperties();
      if (properties != null) {
        Object scope = properties.get(PROPERTY_SCOPE);
        if (scope != null) {
          String scopeStr = String.valueOf(scope);
          return SCOPE_SQL_QUERY.equalsIgnoreCase(scopeStr);
        }
      }
      // Scope not set: legacy behavior, accept based on connection presence
      return true;
    }

    public static boolean isWebApiQuery(Task task) {
      if (!isQueryTask(task)) {
        return false;
      }
      if (task.getConnection() != null || task.getCartography() != null) {
        return false;
      }
      if (task.getService() != null) {
        return true;
      }
      Map<String, Object> properties = task.getProperties();
      if (properties != null) {
        Object scope = properties.get(PROPERTY_SCOPE);
        String scopeStr = String.valueOf(scope);
        return SCOPE_WEB_API_QUERY.equalsIgnoreCase(scopeStr)
            || SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scopeStr);
      }
      return false;
    }

    public static boolean isCartographyQueryTask(Task task) {
      if (!isQueryTask(task)) {
        return false;
      }
      if (task.getCartography() == null) {
        return false;
      }
      // When scope is explicitly set, it must match cartography-query
      Map<String, Object> properties = task.getProperties();
      if (properties != null) {
        Object scope = properties.get(PROPERTY_SCOPE);
        if (scope != null) {
          String scopeStr = String.valueOf(scope);
          return SCOPE_CARTOGRAPHY_QUERY.equalsIgnoreCase(scopeStr);
        }
      }
      // Scope not set: legacy behavior, accept based on cartography presence
      return true;
    }

    public static boolean isUrlQueryTask(Task task) {
      if (!isQueryTask(task)) {
        return false;
      }
      if (task.getConnection() != null || task.getCartography() != null) {
        return false;
      }
      Map<String, Object> properties = task.getProperties();
      if (properties != null) {
        Object scope = properties.get(PROPERTY_SCOPE);
        String scopeStr = String.valueOf(scope);
        return SCOPE_URL_QUERY.equalsIgnoreCase(scopeStr) || SCOPE_URL.equalsIgnoreCase(scopeStr);
      }
      return false;
    }

    public static boolean isCartographyEditionTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_EDIT).equals(taskTypeId(task))
          && task.getCartography() != null;
    }

    private Tasks() {
      // Utility class
    }
  }

  /** Service-related constants. */
  public static class Services {
    public static final String TYPE_WMS = "WMS";
    public static final String TYPE_WMTS = "WMTS";
    public static final String TYPE_WFS = "WFS";

    public static boolean isWfsService(Service service) {
      return service != null && TYPE_WFS.equalsIgnoreCase(service.getType());
    }

    private Services() {
      // Utility class
    }
  }

  /** Application-related constants. */
  public static class Applications {
    /** Code used in DB (STM_APP.APP_TYPE) for touristic applications. */
    public static final String TYPE_TOURISTIC_CODE = "T";

    public static boolean isTouristicApplication(Application app) {
      if (app == null || app.getType() == null) return false;
      String t = app.getType();
      return TYPE_TOURISTIC_CODE.equalsIgnoreCase(t) || "Touristic".equalsIgnoreCase(t);
    }

    private Applications() {
      // Utility class
    }
  }

  /** Tree-related constants. */
  public static class Trees {
    /** Matches TRE_TYPE in DB and API request type for touristic trees. */
    public static final String TYPE_TOURISTIC = "touristic";

    public static boolean isTouristicTree(Tree tree) {
      return tree != null && TYPE_TOURISTIC.equalsIgnoreCase(tree.getType());
    }

    private Trees() {
      // Utility class
    }
  }

  /** Proxy-related constants. */
  public static class Proxy {
    // Resource/connection type keys
    public static final String TYPE_SQL = "SQL";
    public static final String TYPE_API = "API";
    public static final String TYPE_WMS = "WMS";

    // Parameter type for vary parameters
    public static final String PARAM_TYPE_VARY = "VARY";

    private Proxy() {
      // Utility class
    }
  }
}
