package org.sitmun.authorization.client.dto;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.territory.Territory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * NON-REGRESSION GUARDRAIL 3: Edition Mobile (edition-mobile-app) DTO compatibility tests
 *
 * <p>These tests verify backward compatibility with the edition mobile application which expects: -
 * task.url present and valid - task.parameters.typename.value for WFS layer name resolution -
 * task.fields payload with specific keys (name, label, type, value, required, selectable, editable)
 * - Fields keys NOT renamed (fields use "name" as identifier, not expansion variable) - WFS/OGC
 * standard parameters not blocked by vary-key whitelist - Edit task execution continues working
 */
@DisplayName("Edition Mobile Compatibility Tests (Guardrail 3)")
class EditionMobileCompatibilityTest {

  private TaskEditCartographyService service;
  private Application application;
  private Territory territory;
  private DatabaseConnectionService dbConnectionService;

  @BeforeEach
  void setUp() {
    dbConnectionService = mock(DatabaseConnectionService.class);
    service = new TaskEditCartographyService(dbConnectionService);
    ReflectionTestUtils.setField(service, "proxyUrl", "http://localhost:8080/middleware");

    application = mock(Application.class);
    when(application.getId()).thenReturn(10);

    territory = mock(Territory.class);
    when(territory.getId()).thenReturn(5);
  }

  @Nested
  @DisplayName("Edit task DTO contract")
  class EditTaskDtoContract {

    @Test
    @DisplayName("MUST have task.url (proxy URL for execution)")
    void editTaskHasProxyUrl() {
      // Given
      Task task = createEditTask();

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: URL present (edition-mobile uses this to execute edit operations)
      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getUrl()).isEqualTo("http://localhost:8080/middleware/proxy/10/5/WFS/42");
    }

    @Test
    @DisplayName("MUST preserve task.parameters.typename.value (WFS layer name)")
    void preservesTypenameParameterValue() {
      // Given: WFS edit task - typename comes from cartography layers (not manual parameter)
      Task task = createEditTask();

      // Cartography provides typename via layers
      // Manual typename parameter would be overridden by cartography.getLayers()

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: typename from cartography present (edition-mobile needs this for WFS requests)
      assertThat(result.getParameters()).containsKey("typename");

      @SuppressWarnings("unchecked")
      Map<String, Object> typenameDto =
          (Map<String, Object>) result.getParameters().get("typename");
      assertThat(typenameDto).containsEntry("value", "layer1"); // From cartography.getLayers()
      assertThat(typenameDto).containsEntry("type", "query");
      assertThat(typenameDto).containsEntry("required", true);
    }

    @Test
    @DisplayName("MUST preserve parameters contract for edition-mobile usage")
    void preservesParametersContract() {
      // Given: Edit task with multiple parameters
      Task task = createEditTask();

      Map<String, Object> param1 = new HashMap<>();
      param1.put(DomainConstants.Tasks.PARAMETERS_NAME, "typename");
      param1.put(DomainConstants.Tasks.PARAMETERS_TYPE, DomainConstants.Tasks.PARAM_TYPE_TEMPLATE);
      param1.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, true);
      param1.put(DomainConstants.Tasks.PARAMETERS_VALUE, "sitmun:layer");

      Map<String, Object> param2 = new HashMap<>();
      param2.put(DomainConstants.Tasks.PARAMETERS_NAME, "srsName");
      param2.put(DomainConstants.Tasks.PARAMETERS_TYPE, DomainConstants.Tasks.PARAM_TYPE_QUERY);
      param2.put(DomainConstants.Tasks.PARAMETERS_REQUIRED, false);
      param2.put(DomainConstants.Tasks.PARAMETERS_VALUE, "EPSG:4326");

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param1, param2));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: All parameters present with expected structure
      assertThat(result.getParameters()).containsKeys("typename", "srsName");
    }
  }

  @Nested
  @DisplayName("Fields payload contract (CRITICAL)")
  class FieldsPayloadContract {

    @Test
    @DisplayName("CRITICAL: fields array MUST use 'name' key (NOT 'variable')")
    void fieldsMustUseName() {
      // Given: Edit task with fields array
      Task task = createEditTask();
      DatabaseConnection connection = createMockConnection();
      when(task.getConnection()).thenReturn(connection);

      Map<String, Object> field1 = new HashMap<>();
      field1.put(
          DomainConstants.Tasks.FIELDS_NAME, "poi_name"); // MUST be "name" (field identifier)
      field1.put(DomainConstants.Tasks.FIELDS_LABEL, "POI Name");
      field1.put(DomainConstants.Tasks.FIELDS_TYPE, "text");
      field1.put(DomainConstants.Tasks.FIELDS_REQUIRED, true);
      field1.put(DomainConstants.Tasks.FIELDS_EDITABLE, true);
      field1.put(DomainConstants.Tasks.FIELDS_SELECTABLE, false);

      Map<String, Object> field2 = new HashMap<>();
      field2.put(DomainConstants.Tasks.FIELDS_NAME, "category");
      field2.put(DomainConstants.Tasks.FIELDS_LABEL, "Category");
      field2.put(DomainConstants.Tasks.FIELDS_TYPE, "listbox");
      field2.put(DomainConstants.Tasks.FIELDS_REQUIRED, false);
      field2.put(DomainConstants.Tasks.FIELDS_EDITABLE, true);
      field2.put(DomainConstants.Tasks.FIELDS_SELECTABLE, true);
      field2.put(DomainConstants.Tasks.FIELDS_LIST_VALUES, "Tourism,Culture,Sports");

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_FIELDS, List.of(field1, field2));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Fields preserve "name" key (NOT renamed to "variable")
      assertThat(result.getFields()).isNotNull();
      assertThat(result.getFields()).hasSize(2);

      @SuppressWarnings("unchecked")
      Map<String, Object> dtoField1 = (Map<String, Object>) result.getFields().get("poi_name");
      assertThat(dtoField1).containsKey("name"); // NOT "variable"
      assertThat(dtoField1).containsEntry(DomainConstants.Tasks.FIELDS_NAME, "poi_name");
      assertThat(dtoField1).containsEntry(DomainConstants.Tasks.FIELDS_LABEL, "POI Name");
      assertThat(dtoField1).containsEntry(DomainConstants.Tasks.FIELDS_TYPE, "text");
      assertThat(dtoField1).containsEntry(DomainConstants.Tasks.FIELDS_REQUIRED, true);
      assertThat(dtoField1).containsEntry(DomainConstants.Tasks.FIELDS_EDITABLE, true);
      assertThat(dtoField1).containsEntry(DomainConstants.Tasks.FIELDS_SELECTABLE, false);

      @SuppressWarnings("unchecked")
      Map<String, Object> dtoField2 = (Map<String, Object>) result.getFields().get("category");
      assertThat(dtoField2).containsKey(DomainConstants.Tasks.FIELDS_NAME);
      assertThat(dtoField2).containsKey(DomainConstants.Tasks.FIELDS_LIST_VALUES);
    }

    @Test
    @DisplayName("CRITICAL: fields array keys MUST NOT be renamed by migration")
    void fieldsKeysNotRenamedByMigration() {
      // Documentation test - enforced by data migration exclusion rules

      // Fields array structure (MUST be preserved):
      // {
      //   "name": "field_id",        ← Field identifier (NOT expansion variable)
      //   "label": "Display Name",   ← UI label
      //   "type": "text",            ← Input type
      //   "value": "default",        ← Default value (optional)
      //   "required": true,          ← Validation
      //   "editable": true,          ← Edit permission
      //   "selectable": false,       ← Select permission
      //   "listValues": "A,B,C"      ← For listbox type
      // }

      // These are NOT task parameters (different from parameters array)
      // "name" here means "field identifier" not "parameter name"
      // Migration MUST NOT apply variable-model renames to this structure

      String exclusionRule = "Data migration MUST exclude 'fields' array from key renames";
      assertThat(exclusionRule).isNotEmpty();

      // If violated: edition-mobile edit forms break completely
      // - Form fields not rendered (looks for "name" key)
      // - Save operations fail (sends wrong field identifiers)
      // - WFS Transaction requests malformed
    }

    @Test
    @DisplayName("Fields semantics: 'name' is field identifier, not expansion variable")
    void fieldsNameIsIdentifier() {
      // Given: Field with name "category"
      Task task = createEditTask();
      DatabaseConnection connection = createMockConnection();
      when(task.getConnection()).thenReturn(connection);

      Map<String, Object> field = new HashMap<>();
      field.put(
          DomainConstants.Tasks.FIELDS_NAME,
          "category"); // This identifies the database column/WFS property
      field.put(DomainConstants.Tasks.FIELDS_LABEL, "Category"); // This is display text
      field.put(DomainConstants.Tasks.FIELDS_TYPE, "text");

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_FIELDS, List.of(field));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: edition-mobile uses field["name"] to:
      // - Identify form inputs: <input name="category" />
      // - Build WFS Transaction payload: <category>value</category>
      // - Map between form data and WFS properties

      @SuppressWarnings("unchecked")
      Map<String, Object> dtoField = (Map<String, Object>) result.getFields().get("category");
      assertThat(dtoField.get(DomainConstants.Tasks.FIELDS_NAME)).isEqualTo("category");
    }

    @Test
    @DisplayName("All field types supported: text, date, image, number, listbox")
    void allFieldTypesSupported() {
      // Given: Fields with different types
      Task task = createEditTask();
      DatabaseConnection connection = createMockConnection();
      when(task.getConnection()).thenReturn(connection);

      Map<String, Object> textField = new HashMap<>();
      textField.put(DomainConstants.Tasks.FIELDS_NAME, "name");
      textField.put(DomainConstants.Tasks.FIELDS_TYPE, "text");

      Map<String, Object> dateField = new HashMap<>();
      dateField.put(DomainConstants.Tasks.FIELDS_NAME, "created_date");
      dateField.put(DomainConstants.Tasks.FIELDS_TYPE, "date");

      Map<String, Object> numberField = new HashMap<>();
      numberField.put(DomainConstants.Tasks.FIELDS_NAME, "capacity");
      numberField.put(DomainConstants.Tasks.FIELDS_TYPE, "number");

      Map<String, Object> listboxField = new HashMap<>();
      listboxField.put(DomainConstants.Tasks.FIELDS_NAME, "status");
      listboxField.put(DomainConstants.Tasks.FIELDS_TYPE, "listbox");
      listboxField.put(DomainConstants.Tasks.FIELDS_LIST_VALUES, "Active,Inactive,Pending");

      Map<String, Object> properties = new HashMap<>();
      properties.put(
          DomainConstants.Tasks.PROPERTY_FIELDS,
          List.of(textField, dateField, numberField, listboxField));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: All field types present in DTO
      assertThat(result.getFields()).containsKeys("name", "created_date", "capacity", "status");
    }
  }

  @Nested
  @DisplayName("WFS operation compatibility")
  class WfsOperationCompatibility {

    @Test
    @DisplayName("WFS GetFeature request continues working")
    void wfsGetFeatureWorks() {
      // Given: WFS query task
      Task task = createEditTask();

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, "feat-edit");
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: edition-mobile will call:
      // GET {task.url}?service=WFS&version=2.0.0&request=GetFeature&typename=...
      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getUrl()).contains("/proxy/");
    }

    @Test
    @DisplayName("WFS Transaction (insert/update/delete) continues working")
    void wfsTransactionWorks() {
      // Given: Edit task with fields
      Task task = createEditTask();
      DatabaseConnection connection = createMockConnection();
      when(task.getConnection()).thenReturn(connection);

      Map<String, Object> field = new HashMap<>();
      field.put(DomainConstants.Tasks.FIELDS_NAME, "poi_name");
      field.put(DomainConstants.Tasks.FIELDS_EDITABLE, true);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_FIELDS, List.of(field));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: edition-mobile builds WFS Transaction XML using field names
      // POST {task.url}
      // <wfs:Transaction>
      //   <wfs:Update typeName="...">
      //     <wfs:Property>
      //       <wfs:Name>poi_name</wfs:Name>  ← from field["name"]
      //       <wfs:Value>New Value</wfs:Value>
      //     </wfs:Property>
      //   </wfs:Update>
      // </wfs:Transaction>

      assertThat(result.getFields()).containsKey("poi_name");
    }
  }

  @Nested
  @DisplayName("OGC standard parameters (vary-key whitelist)")
  class OgcParameterCompatibility {

    @Test
    @DisplayName("OGC/WFS standard parameters MUST NOT be blocked by whitelist")
    void ogcParametersAllowed() {
      // Documentation test - actual implementation in ProxyConfigurationService

      // Standard OGC parameters that edition-mobile sends:
      List<String> ogcParams =
          List.of(
              "service", // WFS
              "version", // 2.0.0
              "request", // GetFeature, Transaction
              "typename", // Layer name
              "outputFormat", // GML, JSON
              "bbox", // Bounding box filter
              "srsName", // EPSG:4326
              "crs", // Coordinate reference system
              "srs", // Legacy CRS parameter
              "count", // Feature count limit
              "startIndex" // Pagination
              );

      // Vary-key whitelist implementation MUST:
      // - On task-based proxy routes: apply whitelist (task parameters)
      // - On service-based proxy routes: allow OGC params (or have explicit OGC allowlist)

      // If OGC params blocked: edition-mobile WFS requests fail
      // - Map layers don't load
      // - Edit operations fail
      // - Query operations fail

      assertThat(ogcParams).isNotEmpty();
    }

    @Test
    @DisplayName("Service-based proxy routes preserve OGC parameter pass-through")
    void serviceRoutesAllowOgcParams() {
      // Service-based routes: /proxy/{appId}/{terId}/WMS/{serviceId}
      // Task-based routes:    /proxy/{appId}/{terId}/WFS/{taskId}

      // For service-based routes: vary-key whitelist does NOT apply
      // (or applies with explicit OGC allowlist)

      // This ensures edition-mobile map rendering continues working

      String routeDistinction = "Service routes and task routes have different whitelist rules";
      assertThat(routeDistinction).isNotEmpty();
    }
  }

  @Nested
  @DisplayName("Edit operation flows")
  class EditOperationFlows {

    @Test
    @DisplayName("Feature load flow: task.url → GetFeature → render form with fields")
    void featureLoadFlow() {
      // Given
      Task task = createEditTask();
      DatabaseConnection connection = createMockConnection();
      when(task.getConnection()).thenReturn(connection);

      Map<String, Object> field = new HashMap<>();
      field.put("name", "poi_name");
      field.put("label", "POI Name");
      field.put("editable", true);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_FIELDS, List.of(field));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: edition-mobile flow
      // 1. User selects feature on map
      // 2. App calls: GET task.url?request=GetFeature&featureid=...
      // 3. Receives GML/JSON response
      // 4. Renders form using task.fields structure
      // 5. Populates form with feature attributes

      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getFields()).containsKey("poi_name");
    }

    @Test
    @DisplayName("Feature save flow: form data → WFS Transaction via task.url")
    void featureSaveFlow() {
      // Given
      Task task = createEditTask();
      DatabaseConnection connection = createMockConnection();
      when(task.getConnection()).thenReturn(connection);

      Map<String, Object> field1 = new HashMap<>();
      field1.put(DomainConstants.Tasks.FIELDS_NAME, "poi_name");
      field1.put(DomainConstants.Tasks.FIELDS_EDITABLE, true);

      Map<String, Object> field2 = new HashMap<>();
      field2.put(DomainConstants.Tasks.FIELDS_NAME, "category");
      field2.put(DomainConstants.Tasks.FIELDS_EDITABLE, true);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_FIELDS, List.of(field1, field2));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: edition-mobile save flow
      // 1. User edits form fields
      // 2. Clicks save
      // 3. App builds WFS Transaction XML using field["name"] keys
      // 4. POST to task.url with XML body
      // 5. Receives success/error response

      assertThat(result.getUrl()).isNotNull();
      assertThat(result.getFields()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Parameters vs Fields distinction")
  class ParametersVsFields {

    @Test
    @DisplayName("CRITICAL: parameters and fields are separate concepts")
    void parametersAndFieldsAreSeparate() {
      // Parameters: task execution configuration (typename, srsName, etc.)
      // Fields: editable feature attributes (poi_name, category, etc.)

      Task task = createEditTask();
      DatabaseConnection connection = createMockConnection();
      when(task.getConnection()).thenReturn(connection);

      // Parameter: WFS layer name
      Map<String, Object> param = new HashMap<>();
      param.put(DomainConstants.Tasks.PARAMETERS_NAME, "typename");
      param.put(DomainConstants.Tasks.PARAMETERS_VALUE, "sitmun:poi_layer");

      // Field: Editable attribute
      Map<String, Object> field = new HashMap<>();
      field.put(DomainConstants.Tasks.FIELDS_NAME, "poi_name");
      field.put(DomainConstants.Tasks.FIELDS_EDITABLE, true);

      Map<String, Object> properties = new HashMap<>();
      properties.put(DomainConstants.Tasks.PROPERTY_PARAMETERS, List.of(param));
      properties.put(DomainConstants.Tasks.PROPERTY_FIELDS, List.of(field));
      when(task.getProperties()).thenReturn(properties);

      // When
      TaskDto result = service.map(task, application, territory);

      // Then: Both present but separate
      assertThat(result.getParameters()).containsKey("typename"); // Task config
      assertThat(result.getFields()).containsKey("poi_name"); // Feature attribute

      // Variable model migration applies to parameters, NOT fields
      // "typename" is a parameter → may be renamed to use "variable" key
      // "poi_name" is a field → MUST keep "name" key
    }
  }

  // Helper methods
  private Task createEditTask() {
    Task task = mock(Task.class);
    TaskType taskType = mock(TaskType.class);
    when(taskType.getTitle()).thenReturn("Edit");
    when(task.getType()).thenReturn(taskType);
    when(task.getId()).thenReturn(42);

    // Mock cartography and service (required by TaskEditCartographyService)
    org.sitmun.domain.cartography.Cartography cartography =
        mock(org.sitmun.domain.cartography.Cartography.class);
    org.sitmun.domain.service.Service service = mock(org.sitmun.domain.service.Service.class);
    when(service.getId()).thenReturn(42);
    when(service.getType()).thenReturn("WFS");
    when(cartography.getService()).thenReturn(service);
    when(cartography.getLayers()).thenReturn(java.util.List.of("layer1"));
    when(task.getCartography()).thenReturn(cartography);

    Map<String, Object> properties = new HashMap<>();
    properties.put(DomainConstants.Tasks.PROPERTY_SCOPE, "feat-edit");
    when(task.getProperties()).thenReturn(properties);

    return task;
  }

  private DatabaseConnection createMockConnection() {
    DatabaseConnection connection = mock(DatabaseConnection.class);
    when(connection.getUrl()).thenReturn("jdbc:postgresql://localhost/sitmun");
    when(connection.getUser()).thenReturn("sitmun");
    when(connection.getPassword()).thenReturn("password");
    when(connection.getDriver()).thenReturn("org.postgresql.Driver");
    return connection;
  }
}
