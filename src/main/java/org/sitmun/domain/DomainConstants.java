package org.sitmun.domain;

import org.sitmun.domain.application.Application;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.tree.Tree;

/**
 * Constants and utility methods for domain entities.
 */
public final class DomainConstants {
  private DomainConstants() {
    // Prevent instantiation
  }

  public static final class Applications {
    private Applications() {
      // Prevent instantiation
    }

    /**
     * Application type identifier for Touristic
     */
    public static final String TYPE_TOURISTIC = "T";

    /**
     * Checks if an application is of type Touristic.
     *
     * @return true if type is Touristic, false otherwise
     */
    public static boolean isTouristicApplication(Application application) {
      return application != null && TYPE_TOURISTIC.equals(application.getType());
    }
  }

  public static final class Trees {
    private Trees() {
      // Prevent instantiation
    }

    /**
     * Application type identifier for Touristic
     */
    public static final String TYPE_TOURISTIC = "touristic";

    /**
     * Checks if a tree is of type Touristic.
     *
     * @param type Type to check
     * @return true if type is Touristic, false otherwise
     */
    public static boolean isTouristicTree(Tree type) {
      return type != null && TYPE_TOURISTIC.equals(type.getType());

    }
  }

  /**
   * Constants and utility methods for Task-related operations.
   */
  public static final class Tasks {
    private Tasks() {
      // Prevent instantiation
    }

    /**
     * Basic task type identifier
     */
    public static final String BASIC = "Basic";
    /**
     * Query task type identifier
     */
    public static final String QUERY = "Query";
    /**
     * Edit task type identifier
     */
    public static final String EDIT = "Edit";
    /**
     * Task property key for scope
     */
    public static final String PROPERTY_SCOPE = "scope";
    /**
     * Task property key for command
     */
    public static final String PROPERTY_COMMAND = "command";
    /**
     * Task property key for parameters
     */
    public static final String PROPERTY_PARAMETERS = "parameters";
    /**
     * Parameter name key
     */
    public static final String PARAMETERS_NAME = "name";
    /**
     * Parameter type key
     */
    public static final String PARAMETERS_TYPE = "type";
    /**
     * Parameter value key
     */
    public static final String PARAMETERS_VALUE = "value";
    /**
     * Parameter required flag key
     */
    public static final String PARAMETERS_REQUIRED = "required";
    /**
     * Cartography query scope identifier
     */
    public static final String SCOPE_CARTOGRAPHY_QUERY = "cartography-query";
    /**
     * SQL query scope identifier
     */
    public static final String SCOPE_SQL_QUERY = "sql-query";
    /**
     * Web API query scope identifier
     */
    public static final String SCOPE_WEB_API_QUERY = "web-api-query";
    /**
     * Feature service edition scope identifier
     */
    public static final String SCOPE_FEATURES_SERVICE_EDIT = "feat-edit";
    /**
     * Database edition scope identifier
     */
    public static final String SCOPE_DATA_BASE_EDIT = "db-edit";
    /**
     * String type identifier
     */
    public static final String TYPE_STRING = "string";
    /**
     * Number type identifier
     */
    public static final String TYPE_NUMBER = "number";
    /**
     * Array type identifier
     */
    public static final String TYPE_ARRAY = "array";
    /**
     * Object type identifier
     */
    public static final String TYPE_OBJECT = "object";
    /**
     * Boolean type identifier
     */
    public static final String TYPE_BOOLEAN = "boolean";
    /**
     * Null type identifier
     */
    public static final String TYPE_NULL = "null";

    /**
     * Checks if a task is of type Basic.
     *
     * @param task Task to check
     * @return true if task is Basic type, false otherwise
     */
    public static boolean isBasicTask(Task task) {
      if (task == null || task.getType() == null) {
        return false;
      }
      return BASIC.equals(task.getType().getTitle());
    }

    /**
     * Checks if a task is of type Query.
     *
     * @param task Task to check
     * @return true if task is Query type, false otherwise
     */
    public static boolean isQueryTask(Task task) {
      if (task == null || task.getType() == null) {
        return false;
      }
      return QUERY.equals(task.getType().getTitle());
    }

    /**
     * Checks if a task is of type Edition.
     *
     * @param task Task to check
     * @return true if task is Edition type, false otherwise
     */
    public static boolean isEditionTask(Task task) {
      if (task == null || task.getType() == null) {
        return false;
      }
      return EDIT.equals(task.getType().getTitle());
    }

    /**
     * Checks if a task is a Cartography Query task.
     *
     * @param task Task to check
     * @return true if task is a Cartography Query task, false otherwise
     */
    public static boolean isCartographyQueryTask(Task task) {
      if (isQueryTask(task)) {
        if (task.getProperties() == null) {
          return false;
        }
        return SCOPE_CARTOGRAPHY_QUERY.equals(task.getProperties().get(PROPERTY_SCOPE));
      }
      return false;
    }

    /**
     * Checks if a task is a SQL Query task.
     *
     * @param task Task to check
     * @return true if task is a SQL Query task, false otherwise
     */
    public static boolean isSqlQueryTask(Task task) {
      if (isQueryTask(task)) {
        if (task.getProperties() == null) {
          return false;
        }
        return SCOPE_SQL_QUERY.equals(task.getProperties().get(PROPERTY_SCOPE));
      }
      return false;
    }

    /**
     * Checks if a task is a Web API Query task.
     *
     * @param task Task to check
     * @return true if task is a Web API Query task, false otherwise
     */
    public static boolean isWebApiQuery(Task task) {
      if (isQueryTask(task)) {
        if (task.getProperties() == null) {
          return false;
        }
        return SCOPE_WEB_API_QUERY.equals(task.getProperties().get(PROPERTY_SCOPE));
      }
      return false;
    }

    /**
     * Checks if a task is a Cartography Edition task.
     *
     * @param task Task to check
     * @return true if task is a Cartography Edition task, false otherwise
     */
    public static boolean isCartographyEditionTask(Task task) {
      if (isEditionTask(task)) {
        if (task.getProperties() == null) {
          return false;
        }
        return SCOPE_FEATURES_SERVICE_EDIT.equals(task.getProperties().get(PROPERTY_SCOPE));
      }
      return false;
    }
  }

  /**
   * Constants and utility methods for Service-related operations.
   */
  public static final class Services {
    private Services() {
      // Prevent instantiation
    }

    /**
     * WFS service type identifier
     */
    public static final String TYPE_WFS = "WFS";

    /**
     * Checks if a service is of type WFS.
     *
     * @param service Service to check
     * @return true if service is WFS type, false otherwise
     */
    public static boolean isWfsService(Service service) {
      if (service == null) {
        return false;
      }
      return TYPE_WFS.equals(service.getType());
    }
  }
}
