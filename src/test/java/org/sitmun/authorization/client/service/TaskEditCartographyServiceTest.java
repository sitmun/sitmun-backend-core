package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sitmun.domain.DomainConstants.Tasks.FIELDS_NAME;
import static org.sitmun.domain.DomainConstants.Tasks.PROPERTY_FIELDS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.cartography.Cartography;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.territory.Territory;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("TaskEditCartographyService")
class TaskEditCartographyServiceTest {

  private static final List<String> ADMIN_FIELD_ORDER =
      List.of(
          "nom",
          "nom_cast",
          "nom_angl",
          "tipus",
          "data_visita",
          "num_visitants",
          "valoracio",
          "foto",
          "observacions",
          "creat_per",
          "data_alta");

  private final ObjectMapper objectMapper = new JsonMapper();

  private TaskEditCartographyService service;

  @BeforeEach
  void setUp() {
    service =
        new TaskEditCartographyService(
            mock(DatabaseConnectionService.class),
            new TaskParameterProcessor(mock(SystemVariableResolver.class)));
  }

  @Test
  @DisplayName("fields keep the admin array order in the profile map and JSON object")
  void fieldsKeepAdminOrder() throws JacksonException {
    Task task = editTask(ADMIN_FIELD_ORDER);

    TaskDto result =
        service.map(task, Application.builder().id(20).build(), Territory.builder().id(4).build());

    assertThat(result.getFields().keySet()).containsExactlyElementsOf(ADMIN_FIELD_ORDER);

    String json = objectMapper.writeValueAsString(result.getFields());
    assertThat(objectMapper.readTree(json).isObject()).isTrue();
    assertThat(objectMapper.readTree(json).propertyNames())
        .containsExactlyElementsOf(ADMIN_FIELD_ORDER);
  }

  private static Task editTask(List<String> fieldNames) {
    List<Map<String, Object>> fields = new ArrayList<>();
    for (String name : fieldNames) {
      Map<String, Object> field = new HashMap<>();
      field.put(FIELDS_NAME, name);
      fields.add(field);
    }
    Map<String, Object> properties = new HashMap<>();
    properties.put(PROPERTY_FIELDS, fields);
    return Task.builder()
        .id(32344)
        .properties(properties)
        .cartography(
            Cartography.builder()
                .id(1)
                .layers(List.of("proves_edicio:punts_prova"))
                .service(Service.builder().id(1).type("WFS").build())
                .build())
        .build();
  }
}
