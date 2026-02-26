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
    public static final String PROPERTY_MAPPING = "mapping";
    public static final String PROPERTY_FIELDS = "fields";
    public static final String PROPERTY_SCOPE = "scope";

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
    public static final String FIELD_TYPE_DATE = "date";
    public static final String FIELD_TYPE_NUMBER = "number";
    public static final String FIELD_TYPE_IMAGE = "image";
    public static final String FIELD_TYPE_LISTBOX = "listbox";

    // Scope types (more-info tasks)
    public static final String SCOPE_URL = "URL";
    public static final String SCOPE_API = "API";
    public static final String SCOPE_SQL = "SQL";
    public static final String SCOPE_IFRAME = "IFRAME";
    public static final String SCOPE_INFORME = "INFORME";

    // Scope types (query tasks)
    public static final String SCOPE_SQL_QUERY = "sql-query";
    public static final String SCOPE_WEB_API_QUERY = "web-api-query";

    // HTTP API task properties
    public static final String PROPERTY_BODY = "body";
    public static final String PROPERTY_AUTHENTICATION_MODE = "authenticationMode";
    public static final String PROPERTY_USER = "user";
    public static final String PROPERTY_PASSWORD = "password";
    public static final String PROPERTY_HEADERS = "headers";

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
    public static final int TASK_TYPE_ID_QUERY = 5;
    public static final int TASK_TYPE_ID_MORE_INFO = 6;

    // Proxy types
    public static final String PROXY_TYPE_SQL = "sql";
    public static final String PROXY_TYPE_HTTP = "http";

    private static Integer taskTypeId(Task task) {
      return task.getType() != null ? task.getType().getId() : null;
    }

    public static boolean isBasicTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_BASIC).equals(taskTypeId(task));
    }

    public static boolean isMoreInfoTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_MORE_INFO).equals(taskTypeId(task));
    }

    public static boolean isSqlQueryTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_QUERY).equals(taskTypeId(task))
          && task.getConnection() != null;
    }

    public static boolean isWebApiQuery(Task task) {
      if (!Integer.valueOf(TASK_TYPE_ID_QUERY).equals(taskTypeId(task))) {
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
        return SCOPE_WEB_API_QUERY.equalsIgnoreCase(String.valueOf(scope));
      }
      return false;
    }

    public static boolean isCartographyQueryTask(Task task) {
      return Integer.valueOf(TASK_TYPE_ID_QUERY).equals(taskTypeId(task))
          && task.getCartography() != null;
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
    public static final String TYPE_API = "API";

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

  /** System variable registry constants. */
  public static class SystemVariables {
    public static final String PREFIX = "#{";
    public static final String SUFFIX = "}";

    private SystemVariables() {
      // Utility class
    }
  }

  /** Proxy-related constants. */
  public static class Proxy {
    // Vary key modes
    public static final String VARY_KEY_MODE_MONITOR = "MONITOR";
    public static final String VARY_KEY_MODE_ENFORCE = "ENFORCE";

    // Resource/connection type keys
    public static final String TYPE_SQL = "SQL";
    public static final String TYPE_API = "API";
    public static final String TYPE_WMS = "WMS";
    public static final String TYPE_WFS = "WFS";

    // Parameter type for vary parameters
    public static final String PARAM_TYPE_VARY = "VARY";

    private Proxy() {
      // Utility class
    }
  }
}
