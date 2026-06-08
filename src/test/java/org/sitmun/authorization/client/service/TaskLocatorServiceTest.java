package org.sitmun.authorization.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sitmun.domain.DomainConstants.Tasks.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sitmun.authorization.client.dto.TaskDto;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.task.MoreInfoTaskResolver;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.parameter.TaskParameter;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;
import org.sitmun.domain.task.parameter.TaskParameterType;
import org.sitmun.domain.task.type.TaskType;
import org.sitmun.domain.task.ui.TaskUI;
import org.sitmun.domain.territory.Territory;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskLocatorService unit tests")
class TaskLocatorServiceTest {

  private static final String PROXY_BASE = "http://proxy.test";

  @Mock private MoreInfoTaskResolver moreInfoTaskResolver;
  @Mock private TaskParameterProcessor taskParameterProcessor;

  @InjectMocks private TaskLocatorService taskLocatorService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(taskLocatorService, "proxyUrl", PROXY_BASE);
  }

  @Test
  @DisplayName("accept returns true only for locator task type")
  void acceptReturnsTrueOnlyForLocatorType() {
    Task locatorTask = taskWithType(TASK_TYPE_ID_LOCATOR);
    Task queryTask = taskWithType(TASK_TYPE_ID_QUERY);

    assertThat(taskLocatorService.accept(locatorTask)).isTrue();
    assertThat(taskLocatorService.accept(queryTask)).isFalse();
  }

  @Test
  @DisplayName("map exposes raw locator parameters and proxy URL uses locator task id")
  void mapExposesRawParametersAndLocatorProxyUrl() {
    Application application = Application.builder().id(1).build();
    Territory territory = Territory.builder().id(2).build();

    Map<String, Object> queryProperties = new HashMap<>();
    queryProperties.put(PROPERTY_SCOPE, SCOPE_SQL_QUERY);
    queryProperties.put(PROPERTY_COMMAND, "select 1");

    Task queryTask = mock(Task.class);
    when(queryTask.getProperties()).thenReturn(queryProperties);

    Task locatorTask = mock(Task.class);
    when(locatorTask.getId()).thenReturn(41);
    when(locatorTask.getName()).thenReturn("Events locator");

    TaskUI ui = mock(TaskUI.class);
    when(ui.getName()).thenReturn("sitna.search");
    when(ui.getType()).thenReturn("simple");
    when(locatorTask.getUi()).thenReturn(ui);

    Map<String, Object> locatorProperties = new HashMap<>();
    locatorProperties.put(
        PROPERTY_PARAMETERS,
        List.of(
            Map.of("variable", "resultsPath", "value", "/features"),
            Map.of("variable", "labelField", "value", "name")));
    when(locatorTask.getProperties()).thenReturn(locatorProperties);

    TaskParameter resultsPath =
        new TaskParameter(
            "resultsPath", "/features", null, "I", false, false, "Results path", null, Map.of());
    TaskParameter labelField =
        new TaskParameter("labelField", "name", null, "I", false, false, "Label", null, Map.of());

    when(taskParameterProcessor.parse(locatorTask)).thenReturn(List.of(resultsPath, labelField));
    when(taskParameterProcessor.classify(any(TaskParameter.class)))
        .thenReturn(TaskParameterType.DECLARED_WITH_DEFAULT);
    when(moreInfoTaskResolver.findRelatedQueryTask(locatorTask)).thenReturn(Optional.of(queryTask));

    TaskDto dto = taskLocatorService.map(locatorTask, application, territory);

    assertThat(dto.getId()).isEqualTo(TASK_PROFILE_ID_PREFIX + "41");
    assertThat(dto.getUiControl()).isEqualTo("sitna.search");
    assertThat(dto.getScope()).isEqualTo(SCOPE_SQL);
    assertThat(dto.getUrl()).isEqualTo(PROXY_BASE + "/proxy/1/2/SQL/41");
    assertThat(dto.getParameters()).containsEntry("resultsPath", "/features");
    assertThat(dto.getParameters()).containsEntry("labelField", "name");
    assertThat(dto.getParameters().get("resultsPath")).isInstanceOf(String.class);
  }

  private static Task taskWithType(int typeId) {
    TaskType type = mock(TaskType.class);
    when(type.getId()).thenReturn(typeId);
    Task task = mock(Task.class);
    when(task.getType()).thenReturn(type);
    return task;
  }
}
