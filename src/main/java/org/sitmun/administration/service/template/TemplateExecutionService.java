package org.sitmun.administration.service.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderedTaskDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.template.childdata.ChildDataOutcome;
import org.sitmun.administration.service.template.childdata.ChildDataRequest;
import org.sitmun.administration.service.template.childdata.ChildDataResult;
import org.sitmun.administration.service.template.childdata.PrincipalKind;
import org.sitmun.administration.service.template.childdata.TemplateChildDataPort;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.task.relation.TaskRelationRepository;
import org.sitmun.infrastructure.security.core.SecurityConstants;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateExecutionService {

  private static final int MAX_TEMPLATE_NESTING_LEVEL = 3;
  private static final String TEMPLATE_NESTING_DEPTH_EXCEEDED_PREFIX =
      "Template nesting depth exceeded";
  private static final String NO_DATA_LITERAL = "Sense dades";
  private static final Pattern REFERENCE_ALIAS_PATTERN =
      Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
  private static final TypeReference<List<Object>> ARRAY_TYPE_REFERENCE = new TypeReference<>() {};
  private static final TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE =
      new TypeReference<>() {};
  public static final String TEMPLATE_CHILD_TASK_PARAMETERS = "templateChildTaskParameters";
  public static final String PARAMETERS = "parameters";
  public static final String CHILD_TASK_PARAMETERS = "childTaskParameters";
  public static final String VALUE = "value";
  public static final String DIV_CLOSING_TAG = "</div>";
  public static final String TEMPLATE = "template";
  public static final String TABLE = "table";
  public static final String COMPLETED = "COMPLETED";
  public static final String ORDER = "order";
  public static final String SCROLL = "scroll";

  private final TaskRepository taskRepository;
  private final RoleRepository roleRepository;
  private final TaskRelationRepository taskRelationRepository;
  private final TemplateRenderService templateRenderService;
  private final TemplateRequestCoordinatesService templateRequestCoordinatesService;
  private final UserApplicationAccessPolicy userApplicationAccessPolicy;
  private final TemplateChildDataPort templateChildDataPort;
  private final ObjectMapper objectMapper;

  public TemplateTaskExecutionResponseDto executeLinkedTask(
      TemplateTaskExecutionRequestDto requestDto) {
    Task task =
        taskRepository
            .findById(requestDto.getLinkedTaskId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Integer rootTemplateTaskId = requestDto.getTemplateTaskId();
    if (rootTemplateTaskId == null
        && task.getType() != null
        && Integer.valueOf(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
            .equals(task.getType().getId())) {
      rootTemplateTaskId = task.getId();
    }

    RequestCoordinates coordinates =
        requestDto.getAppId() != null && requestDto.getTerId() != null
            ? templateRequestCoordinatesService.build(requestDto.getAppId(), requestDto.getTerId())
            : templateRequestCoordinatesService.buildForCurrentUser();
    Map<String, Map<String, Object>> childTaskParameters =
        requestDto.getChildTaskParameters() == null
            ? Collections.emptyMap()
            : requestDto.getChildTaskParameters();

    if (!mayAccessTask(task, coordinates, true)) {
      return buildNoDataExecutionResponse(task);
    }

    return executeTask(
        task,
        stringifyParameters(requestDto.getParameters()),
        childTaskParameters,
        rootTemplateTaskId,
        coordinates,
        0,
        false,
        true);
  }

  private TemplateTaskExecutionResponseDto buildNoDataExecutionResponse(Task task) {
    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status(COMPLETED)
        .resultType(TABLE)
        .context(buildChildNoDataContext())
        .rows(Collections.emptyList())
        .resourceUrl(null)
        .build();
  }

  @Transactional(readOnly = true)
  public MoreInfoAdvancedRenderResponseDto renderMoreInfoAdvanced(
      MoreInfoAdvancedRenderRequestDto requestDto) {
    RequestCoordinates coordinates =
        templateRequestCoordinatesService.build(requestDto.getAppId(), requestDto.getTerId());
    String username = resolveAuthorizedUsername(coordinates);
    if (userApplicationAccessPolicy.isPrivateAppDeniedForPublic(username, requestDto.getAppId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    List<Integer> miaTaskIds =
        requestDto.getMiaTaskIds() == null ? List.of() : requestDto.getMiaTaskIds();
    Map<String, Object> featureParameters =
        requestDto.getParameters() == null ? Collections.emptyMap() : requestDto.getParameters();

    List<MoreInfoAdvancedRenderedTaskDto> renderedTasks = new ArrayList<>();
    for (Integer miaTaskId : miaTaskIds) {
      Task miaTask =
          taskRepository
              .findById(miaTaskId)
              .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
      if (!DomainConstants.Tasks.isMoreInfoAdvancedTask(miaTask)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is not a MIA task");
      }
      if (!mayAccessTask(miaTask, coordinates, false)) {
        if (miaTaskIds.size() == 1) {
          throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        continue;
      }
      renderedTasks.add(renderSingleMoreInfoAdvancedTask(miaTask, featureParameters, coordinates));
    }
    if (renderedTasks.isEmpty() && !miaTaskIds.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    return MoreInfoAdvancedRenderResponseDto.builder().tasks(renderedTasks).build();
  }

  private MoreInfoAdvancedRenderedTaskDto renderSingleMoreInfoAdvancedTask(
      Task miaTask, Map<String, Object> featureParameters, RequestCoordinates coordinates) {
    Map<String, Object> properties =
        miaTask.getProperties() == null ? Collections.emptyMap() : miaTask.getProperties();
    Map<String, Object> miaParameters = convertBasicParameters(properties);
    String visualizationMode =
        SCROLL.equals(miaParameters.get("visualizationMode"))
                || SCROLL.equals(properties.get("parentLayout"))
            ? SCROLL
            : "tabs";
    List<Map<String, Object>> includedTasks = readIncludedTasks(miaTask, miaParameters);

    String html =
        "tabs".equals(visualizationMode)
            ? renderMiaChildrenAsTabs(miaTask, includedTasks, featureParameters, coordinates)
            : renderMiaChildrenAsScroll(includedTasks, featureParameters, coordinates);

    return MoreInfoAdvancedRenderedTaskDto.builder()
        .taskId(miaTask.getId())
        .title(miaTask.getName())
        .html(html)
        .build();
  }

  private String renderMiaChildrenAsTabs(
      Task miaTask,
      List<Map<String, Object>> includedTasks,
      Map<String, Object> featureParameters,
      RequestCoordinates coordinates) {
    String renderId = "mia-backend-" + miaTask.getId();
    StringBuilder tabs = new StringBuilder();
    StringBuilder panels = new StringBuilder();

    for (int index = 0; index < includedTasks.size(); index++) {
      Map<String, Object> childDefinition = includedTasks.get(index);
      String panelId = renderId + "-" + index;
      String active = index == 0 ? " sitmun-mia-tab-active" : "";
      String hidden = index == 0 ? "" : " style=\"display:none\"";
      tabs.append("<button class=\"sitmun-mia-tab")
          .append(active)
          .append("\" data-mia-tab=\"")
          .append(panelId)
          .append("\">")
          .append(escapeHtml(resolveChildTitle(childDefinition, index)))
          .append("</button>");
      panels
          .append("<div class=\"sitmun-mia-tab-panel\" data-mia-panel=\"")
          .append(panelId)
          .append("\"")
          .append(hidden)
          .append(">")
          .append(renderMiaChild(childDefinition, featureParameters, coordinates))
          .append(DIV_CLOSING_TAG);
    }

    return "<div class=\"sitmun-mia-tabs-bar\" data-mia-tabs=\""
        + renderId
        + "\">"
        + tabs
        + "</div><div class=\"sitmun-mia-body\">"
        + panels
        + DIV_CLOSING_TAG;
  }

  private String renderMiaChildrenAsScroll(
      List<Map<String, Object>> includedTasks,
      Map<String, Object> featureParameters,
      RequestCoordinates coordinates) {
    StringBuilder sections = new StringBuilder();
    for (int index = 0; index < includedTasks.size(); index++) {
      Map<String, Object> childDefinition = includedTasks.get(index);
      sections
          .append(
              "<div class=\"sitmun-mia-scroll-section\"><div class=\"sitmun-mia-section-title\">")
          .append(escapeHtml(resolveChildTitle(childDefinition, index)))
          .append(DIV_CLOSING_TAG)
          .append(renderMiaChild(childDefinition, featureParameters, coordinates))
          .append(DIV_CLOSING_TAG);
    }
    return "<div class=\"sitmun-mia-body sitmun-mia-scroll-body\">" + sections + DIV_CLOSING_TAG;
  }

  @SuppressWarnings("unchecked")
  private String renderMiaChild(
      Map<String, Object> childDefinition,
      Map<String, Object> featureParameters,
      RequestCoordinates coordinates) {
    Integer childTaskId = parseTaskId(childDefinition.get("id"));
    if (childTaskId == null) {
      return "<div class=\"sitmun-mia-error\">Invalid child task id</div>";
    }

    Task childTask =
        taskRepository
            .findById(childTaskId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!mayAccessTask(childTask, coordinates, false)) {
      return renderNoDataHtml();
    }
    Map<String, String> childParameters =
        stringifyParameters(
            resolveMappedParameters(childDefinition.get(PARAMETERS), featureParameters));
    if (childParameters.isEmpty()) {
      childParameters =
          stringifyParameters(
              resolveMappedParameters(readMiaChildParameterMappings(childTask), featureParameters));
    }
    Map<String, Map<String, Object>> childTaskParameters =
        new LinkedHashMap<>(
            resolveMappedChildTaskParameters(
                childDefinition.get(CHILD_TASK_PARAMETERS), featureParameters));
    Integer rootTemplateTaskId =
        childTask.getType() != null
                && Integer.valueOf(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                    .equals(childTask.getType().getId())
            ? childTask.getId()
            : null;
    if (rootTemplateTaskId != null) {
      mergeMappedTemplateChildTaskParameters(
          childTaskParameters,
          childDefinition.get(TEMPLATE_CHILD_TASK_PARAMETERS),
          rootTemplateTaskId,
          featureParameters);
      enrichTemplateChildTaskParameters(childTask, childTaskParameters, featureParameters, 0);
    }
    TemplateTaskExecutionResponseDto result;
    boolean isTemplateChild = rootTemplateTaskId != null;
    try {
      result =
          executeTask(
              childTask,
              childParameters,
              childTaskParameters,
              rootTemplateTaskId,
              coordinates,
              0,
              isTemplateChild,
              false);
    } catch (ResponseStatusException exception) {
      if (isTemplateChild || isTemplateNestingDepthExceeded(exception)) {
        throw exception;
      }
      String taskName =
          childTask.getName() != null ? childTask.getName() : String.valueOf(childTaskId);
      return "<div class=\"sitmun-mia-error\">Error ejecutando tarea: "
          + escapeHtml(taskName)
          + DIV_CLOSING_TAG;
    }

    if (TEMPLATE.equals(result.getResultType())) {
      Object html = result.getContext() != null ? result.getContext().get("html") : null;
      return html == null ? "" : String.valueOf(html);
    }
    if (TABLE.equals(result.getResultType())) {
      return renderRowsAsTable(result.getRows());
    }
    if (result.getResourceUrl() != null) {
      String url = escapeHtml(result.getResourceUrl());
      return "<a href=\""
          + url
          + "\" target=\"_blank\" rel=\"noopener noreferrer\">"
          + url
          + "</a>";
    }
    return renderNoDataHtml();
  }

  private void enrichTemplateChildTaskParameters(
      Task templateTask,
      Map<String, Map<String, Object>> childTaskParameters,
      Map<String, Object> featureParameters,
      int depth) {
    List<TaskRelation> relations = taskRelationRepository.findByTaskId(templateTask.getId());
    for (TaskRelation relation : relations) {
      if (!List.of("template-task", "template-nested").contains(relation.getRelationType())) {
        continue;
      }
      Task relatedTask = relation.getRelatedTask();
      Map<String, Object> resolvedParameters =
          resolveMappedParameters(readMiaChildParameterMappings(relatedTask), featureParameters);
      if (!resolvedParameters.isEmpty()) {
        Map<String, Object> existingParameters =
            childTaskParameters.computeIfAbsent(
                String.valueOf(relatedTask.getId()), ignored -> new LinkedHashMap<>());
        resolvedParameters.forEach(existingParameters::putIfAbsent);
      }
      if (isTemplateTask(relatedTask) && depth + 1 < MAX_TEMPLATE_NESTING_LEVEL) {
        enrichTemplateChildTaskParameters(
            relatedTask, childTaskParameters, featureParameters, depth + 1);
      }
    }
  }

  private void mergeMappedTemplateChildTaskParameters(
      Map<String, Map<String, Object>> childTaskParameters,
      Object rawTemplateChildTaskParameters,
      Integer templateTaskId,
      Map<String, Object> featureParameters) {
    Object rawInnerTaskParameters =
        selectTemplateChildTaskParameters(rawTemplateChildTaskParameters, templateTaskId);
    Map<String, Map<String, Object>> resolvedParameters =
        resolveMappedChildTaskParameters(rawInnerTaskParameters, featureParameters);
    resolvedParameters.forEach(
        (taskId, parameters) -> {
          Map<String, Object> existingParameters =
              childTaskParameters.computeIfAbsent(taskId, ignored -> new LinkedHashMap<>());
          parameters.forEach(existingParameters::putIfAbsent);
        });
  }

  private Object selectTemplateChildTaskParameters(
      Object rawTemplateChildTaskParameters, Integer templateTaskId) {
    if (!(rawTemplateChildTaskParameters instanceof Map<?, ?> templateChildTaskParameters)) {
      return Collections.emptyMap();
    }
    Object exactTemplateMapping = getMapValueByTaskId(templateChildTaskParameters, templateTaskId);
    if (exactTemplateMapping instanceof Map<?, ?>) {
      return exactTemplateMapping;
    }
    return rawTemplateChildTaskParameters;
  }

  private boolean isTemplateTask(Task task) {
    return task.getType() != null
        && Integer.valueOf(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
            .equals(task.getType().getId());
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> readIncludedTasks(
      Task miaTask, Map<String, Object> miaParameters) {
    Object rawIncludedTasks = miaParameters.get("includedTasks");
    if (!(rawIncludedTasks instanceof List<?> rawList)) {
      return readIncludedTasksFromChildOrder(miaTask);
    }
    List<Map<String, Object>> includedTasks = new ArrayList<>();
    for (Object rawItem : rawList) {
      if (rawItem instanceof Map<?, ?> rawMap) {
        includedTasks.add((Map<String, Object>) rawMap);
      }
    }
    includedTasks.sort(
        (left, right) ->
            Integer.compare(toInt(left.get(ORDER), 999), toInt(right.get(ORDER), 999)));
    attachTemplateChildTaskParameters(miaTask, miaParameters, includedTasks);
    return includedTasks;
  }

  private void attachTemplateChildTaskParameters(
      Task miaTask, Map<String, Object> miaParameters, List<Map<String, Object>> includedTasks) {
    Map<String, Object> properties =
        miaTask.getProperties() == null ? Collections.emptyMap() : miaTask.getProperties();
    Object rawTemplateChildTaskParameters =
        miaParameters.containsKey(TEMPLATE_CHILD_TASK_PARAMETERS)
            ? miaParameters.get(TEMPLATE_CHILD_TASK_PARAMETERS)
            : properties.get(TEMPLATE_CHILD_TASK_PARAMETERS);
    if (!(rawTemplateChildTaskParameters instanceof Map<?, ?> templateChildTaskParameters)) {
      return;
    }
    for (Map<String, Object> includedTask : includedTasks) {
      Integer childTaskId = parseTaskId(includedTask.get("id"));
      if (childTaskId == null || includedTask.containsKey(TEMPLATE_CHILD_TASK_PARAMETERS)) {
        continue;
      }
      Object templateMapping = getMapValueByTaskId(templateChildTaskParameters, childTaskId);
      if (templateMapping instanceof Map<?, ?> && !((Map<?, ?>) templateMapping).isEmpty()) {
        includedTask.put(TEMPLATE_CHILD_TASK_PARAMETERS, templateMapping);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> readIncludedTasksFromChildOrder(Task miaTask) {
    Map<String, Object> properties =
        miaTask.getProperties() == null ? Collections.emptyMap() : miaTask.getProperties();
    Object rawChildOrder = properties.get("childTaskOrderIds");
    if (!(rawChildOrder instanceof List<?> childOrder) || childOrder.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, Object> adminChildTaskParameters =
        properties.get(CHILD_TASK_PARAMETERS) instanceof Map<?, ?> ctp
            ? (Map<String, Object>) ctp
            : Collections.emptyMap();
    Map<String, Object> adminTemplateChildTaskParameters =
        properties.get(TEMPLATE_CHILD_TASK_PARAMETERS) instanceof Map<?, ?> tctp
            ? (Map<String, Object>) tctp
            : Collections.emptyMap();

    List<Map<String, Object>> includedTasks = new ArrayList<>();
    for (int index = 0; index < childOrder.size(); index++) {
      int order = index;
      Integer childTaskId = parseTaskId(childOrder.get(index));
      if (childTaskId == null) {
        continue;
      }
      taskRepository
          .findById(childTaskId)
          .ifPresent(
              childTask -> {
                Map<String, Object> childDefinition = new LinkedHashMap<>();
                childDefinition.put("id", childTask.getId());
                childDefinition.put("name", childTask.getName());
                childDefinition.put(ORDER, order);
                childDefinition.put(
                    "childType",
                    childTask.getType() != null
                            && Integer.valueOf(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                                .equals(childTask.getType().getId())
                        ? TEMPLATE
                        : "query");
                Object explicitMapping =
                    getMapValueByTaskId(adminChildTaskParameters, childTask.getId());
                if (explicitMapping instanceof Map<?, ?>
                    && !((Map<?, ?>) explicitMapping).isEmpty()) {
                  childDefinition.put(PARAMETERS, explicitMapping);
                } else {
                  childDefinition.put(PARAMETERS, readMiaChildParameterMappings(childTask));
                }
                Object templateChildTaskMapping =
                    getMapValueByTaskId(adminTemplateChildTaskParameters, childTask.getId());
                if (templateChildTaskMapping instanceof Map<?, ?>
                    && !((Map<?, ?>) templateChildTaskMapping).isEmpty()) {
                  childDefinition.put(TEMPLATE_CHILD_TASK_PARAMETERS, templateChildTaskMapping);
                }
                includedTasks.add(childDefinition);
              });
    }
    return includedTasks;
  }

  private Object getMapValueByTaskId(Map<?, ?> valuesByTaskId, Integer taskId) {
    if (valuesByTaskId == null || taskId == null) {
      return null;
    }
    if (valuesByTaskId.containsKey(taskId)) {
      return valuesByTaskId.get(taskId);
    }
    return valuesByTaskId.get(String.valueOf(taskId));
  }

  private Map<String, Object> readMiaChildParameterMappings(Task childTask) {
    Map<String, Object> mappings = new LinkedHashMap<>();
    if (isTemplateTask(childTask)) {
      return mappings;
    }
    Map<String, Object> properties =
        childTask.getProperties() == null ? Collections.emptyMap() : childTask.getProperties();
    Object rawParameters = properties.get(DomainConstants.Tasks.PROPERTY_PARAMETERS);
    if (!(rawParameters instanceof List<?> parameters)) {
      return mappings;
    }
    for (Object rawParameter : parameters) {
      if (!(rawParameter instanceof Map<?, ?> parameter)) {
        continue;
      }
      Object name = parameter.get(DomainConstants.Tasks.PARAMETERS_NAME);
      Object field = parameter.get(DomainConstants.Tasks.PARAMETERS_VALUE);
      if (name != null) {
        mappings.put(
            String.valueOf(name), field != null ? String.valueOf(field) : String.valueOf(name));
      }
    }
    return mappings;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> convertBasicParameters(Map<String, Object> properties) {
    if (properties == null) {
      return Collections.emptyMap();
    }
    Object rawParameters = properties.get(DomainConstants.Tasks.PROPERTY_PARAMETERS);
    if (!(rawParameters instanceof List<?> rawList)) {
      return Collections.emptyMap();
    }
    Map<String, Object> converted = new LinkedHashMap<>();
    for (Object rawParameter : rawList) {
      if (!(rawParameter instanceof Map<?, ?> parameter)) {
        continue;
      }
      Object rawName = parameter.get(DomainConstants.Tasks.PARAMETERS_NAME);
      Object rawType = parameter.get(DomainConstants.Tasks.PARAMETERS_TYPE);
      Object rawValue = parameter.get(DomainConstants.Tasks.PARAMETERS_VALUE);
      if (rawName == null || rawType == null) {
        continue;
      }
      converted.put(
          String.valueOf(rawName), convertTypedParameterValue(String.valueOf(rawType), rawValue));
    }
    return converted;
  }

  private Map<String, Object> readTemplateDefaultParameters(Task task) {
    Map<String, Object> properties =
        task.getProperties() == null ? Collections.emptyMap() : task.getProperties();
    Object rawParameters = properties.get(DomainConstants.Tasks.PROPERTY_PARAMETERS);
    if (!(rawParameters instanceof List<?> rawList)) {
      return Collections.emptyMap();
    }

    Map<String, Object> defaults = new LinkedHashMap<>();
    for (Object rawParameter : rawList) {
      if (!(rawParameter instanceof Map<?, ?> parameter)) {
        continue;
      }

      Object rawName = parameter.get(DomainConstants.Tasks.PARAMETERS_NAME);
      if (rawName == null) {
        rawName = parameter.get("variable");
      }
      if (rawName == null) {
        rawName = parameter.get("label");
      }
      if (rawName == null) {
        continue;
      }

      Object rawValue = parameter.get(DomainConstants.Tasks.PARAMETERS_VALUE);
      if (rawValue == null) {
        continue;
      }

      Object rawType = parameter.get(DomainConstants.Tasks.PARAMETERS_TYPE);
      Object convertedValue =
          rawType == null
              ? String.valueOf(rawValue)
              : convertTypedParameterValue(String.valueOf(rawType), rawValue);
      defaults.put(String.valueOf(rawName), convertedValue);
    }
    return defaults;
  }

  private Object convertTypedParameterValue(String type, Object value) {
    String stringValue = value == null ? null : String.valueOf(value);
    try {
      return switch (type) {
        case DomainConstants.Tasks.TYPE_NUMBER ->
            stringValue == null ? null : objectMapper.readValue(stringValue, Number.class);
        case DomainConstants.Tasks.TYPE_ARRAY ->
            stringValue == null
                ? List.of()
                : objectMapper.readValue(stringValue, ARRAY_TYPE_REFERENCE);
        case DomainConstants.Tasks.TYPE_OBJECT ->
            stringValue == null
                ? Map.of()
                : objectMapper.readValue(stringValue, OBJECT_TYPE_REFERENCE);
        case DomainConstants.Tasks.TYPE_BOOLEAN ->
            stringValue == null
                ? Boolean.FALSE
                : objectMapper.readValue(stringValue, Boolean.class);
        case DomainConstants.Tasks.TYPE_NULL -> null;
        default -> stringValue == null ? "" : stringValue;
      };
    } catch (IOException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid MIA parameter value", exception);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> resolveMappedParameters(
      Object rawParameterDefinitions, Map<String, Object> featureParameters) {
    if (!(rawParameterDefinitions instanceof Map<?, ?> parameterDefinitions)) {
      return Collections.emptyMap();
    }
    Map<String, Object> resolved = new LinkedHashMap<>();
    parameterDefinitions.forEach(
        (rawName, rawDefinition) -> {
          if (rawName == null) {
            return;
          }
          Object value = resolveMappedValue(rawName, rawDefinition, featureParameters);
          if (value != null) {
            resolved.put(String.valueOf(rawName), value);
          }
        });
    return resolved;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Map<String, Object>> resolveMappedChildTaskParameters(
      Object rawChildTaskParameters, Map<String, Object> featureParameters) {
    if (!(rawChildTaskParameters instanceof Map<?, ?> childTaskParameters)) {
      return Collections.emptyMap();
    }
    Map<String, Map<String, Object>> resolved = new LinkedHashMap<>();
    childTaskParameters.forEach(
        (rawTaskId, rawParameterDefinitions) -> {
          Map<String, Object> taskParameters =
              resolveMappedParameters(rawParameterDefinitions, featureParameters);
          if (!taskParameters.isEmpty()) {
            resolved.put(String.valueOf(rawTaskId), taskParameters);
          }
        });
    return resolved;
  }

  @SuppressWarnings("unchecked")
  private Object resolveMappedValue(
      Object rawName, Object rawDefinition, Map<String, Object> featureParameters) {
    if (rawDefinition instanceof Map<?, ?> definition) {
      Object fieldPath =
          definition.get(VALUE) != null ? definition.get(VALUE) : definition.get("name");
      if (fieldPath == null) {
        fieldPath = rawName;
      }
      return getValueByPath(featureParameters, String.valueOf(fieldPath));
    }
    return getValueByPath(featureParameters, String.valueOf(rawDefinition));
  }

  private Object getValueByPath(Map<String, Object> data, String path) {
    if (data == null || path == null || path.isBlank()) {
      return null;
    }
    if (data.containsKey(path)) {
      return data.get(path);
    }
    if (!path.contains(".")) {
      return null;
    }
    Object current = data;
    for (String part : path.split("\\.")) {
      if (!(current instanceof Map<?, ?> map)) {
        return null;
      }
      current = map.get(part);
      if (current == null) {
        return null;
      }
    }
    return current;
  }

  private Integer parseTaskId(Object rawId) {
    if (rawId instanceof Number number) {
      return number.intValue();
    }
    if (rawId == null) {
      return null;
    }
    Matcher matcher = Pattern.compile("(?:^|/)\\d+$").matcher(String.valueOf(rawId));
    if (!matcher.find()) {
      return null;
    }
    return Integer.parseInt(matcher.group().replace("/", ""));
  }

  private int toInt(Object value, int fallback) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value == null) {
      return fallback;
    }
    try {
      return Integer.parseInt(String.valueOf(value));
    } catch (NumberFormatException exception) {
      return fallback;
    }
  }

  private String resolveChildTitle(Map<String, Object> childDefinition, int index) {
    Object name = childDefinition.get("name");
    return name == null || String.valueOf(name).isBlank()
        ? "Consulta " + (index + 1)
        : String.valueOf(name);
  }

  private String renderRowsAsTable(List<Map<String, Object>> rows) {
    if (rows == null || rows.isEmpty()) {
      return renderNoDataHtml();
    }
    Set<String> columns = new LinkedHashSet<>();
    rows.forEach(row -> columns.addAll(row.keySet()));
    StringBuilder html = new StringBuilder("<table class=\"sitmun-json-table\"><thead><tr>");
    columns.forEach(column -> html.append("<th>").append(escapeHtml(column)).append("</th>"));
    html.append("</tr></thead><tbody>");
    for (Map<String, Object> row : rows) {
      html.append("<tr>");
      columns.forEach(
          column ->
              html.append("<td>")
                  .append(
                      escapeHtml(row.get(column) == null ? "" : String.valueOf(row.get(column))))
                  .append("</td>"));
      html.append("</tr>");
    }
    html.append("</tbody></table>");
    return html.toString();
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private boolean mayAccessTask(Task task, RequestCoordinates coordinates, boolean adminGodMode) {
    if (task == null) {
      return true;
    }
    if (adminGodMode && currentUserHasRole("ROLE_ADMIN")) {
      return true;
    }
    if (coordinates == null) {
      return false;
    }
    if (!currentUserIsAuthenticated()) {
      return false;
    }
    String username = resolveAuthorizedUsername(coordinates);
    Integer applicationId =
        coordinates.getApplication() != null ? coordinates.getApplication().getId() : null;
    Integer territoryId =
        coordinates.getTerritory() != null ? coordinates.getTerritory().getId() : null;
    if (!StringUtils.hasText(username) || applicationId == null || territoryId == null) {
      return false;
    }

    List<Role> roles =
        roleRepository.findRolesByApplicationAndUserAndTerritory(
            username, applicationId, territoryId);
    return !roles.isEmpty()
        && taskRepository.findByRolesAndTerritory(roles, territoryId).stream()
            .anyMatch(accessibleTask -> task.getId().equals(accessibleTask.getId()));
  }

  private boolean currentUserHasRole(String roleName) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.getAuthorities() != null
        && authentication.getAuthorities().stream()
            .anyMatch(authority -> roleName.equals(authority.getAuthority()));
  }

  private boolean currentUserIsAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && StringUtils.hasText(authentication.getName())
        && !"anonymousUser".equals(authentication.getName());
  }

  private String resolveAuthorizedUsername(RequestCoordinates coordinates) {
    if (coordinates != null
        && coordinates.getUser() != null
        && StringUtils.hasText(coordinates.getUser().getUsername())) {
      return coordinates.getUser().getUsername();
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !StringUtils.hasText(authentication.getName())) {
      return null;
    }
    return authentication.getName();
  }

  private String renderNoDataHtml() {
    return "<div class=\"sitmun-mia-empty\">"
        + escapeHtml(resolveNoDataMessage())
        + DIV_CLOSING_TAG;
  }

  private String resolveNoDataMessage() {
    String language = resolveRequestLanguage();
    if (!StringUtils.hasText(language)) {
      return NO_DATA_LITERAL;
    }
    String normalized = language.trim().toLowerCase();
    if (normalized.startsWith("es")) {
      return "Sin datos";
    }
    if (normalized.startsWith("en")) {
      return "No data";
    }
    if (normalized.startsWith("fr")) {
      return "Aucune donnee";
    }
    return NO_DATA_LITERAL;
  }

  private String resolveRequestLanguage() {
    if (RequestContextHolder.getRequestAttributes()
        instanceof ServletRequestAttributes attributes) {
      String language = attributes.getRequest().getParameter("lang");
      if (StringUtils.hasText(language)) {
        return language;
      }
    }
    return null;
  }

  private TemplateTaskExecutionResponseDto executeTask(
      Task task,
      Map<String, String> parameters,
      Map<String, Map<String, Object>> childTaskParameters,
      Integer rootTemplateTaskId,
      RequestCoordinates coordinates,
      int depth,
      boolean isolateTemplateChildFailures,
      boolean adminGodMode) {
    boolean isTemplateTask =
        task.getType() != null
            && Integer.valueOf(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
                .equals(task.getType().getId());
    if (isTemplateTask && depth >= MAX_TEMPLATE_NESTING_LEVEL) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          TEMPLATE_NESTING_DEPTH_EXCEEDED_PREFIX
              + ". Maximum allowed is "
              + MAX_TEMPLATE_NESTING_LEVEL);
    }

    if (isTemplateTask) {
      return executeTemplateTask(
          task,
          parameters,
          childTaskParameters,
          rootTemplateTaskId,
          coordinates,
          depth + 1,
          isolateTemplateChildFailures,
          adminGodMode);
    }

    String scope = String.valueOf(task.getProperties().get(DomainConstants.Tasks.PROPERTY_SCOPE));
    ChildDataRequest childRequest =
        childDataRequest(task, parameters, coordinates, scope, adminGodMode);
    if (DomainConstants.Tasks.SCOPE_SQL_QUERY.equalsIgnoreCase(scope)) {
      return toExecutionResponse(task, templateChildDataPort.executeSql(childRequest));
    }
    if (DomainConstants.Tasks.SCOPE_WEB_API_QUERY.equalsIgnoreCase(scope)) {
      return toExecutionResponse(task, templateChildDataPort.executeApi(childRequest));
    }
    if (DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_URL_QUERY.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_RESOURCE_QUERY.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_URL.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_RESOURCE.equalsIgnoreCase(scope)) {
      return toExecutionResponse(task, templateChildDataPort.resolveDirect(childRequest));
    }

    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Unsupported linked task scope: " + scope);
  }

  private ChildDataRequest childDataRequest(
      Task task,
      Map<String, String> parameters,
      RequestCoordinates coordinates,
      String scope,
      boolean adminGodMode) {
    Integer appId =
        coordinates != null && coordinates.getApplication() != null
            ? coordinates.getApplication().getId()
            : null;
    Integer terId =
        coordinates != null && coordinates.getTerritory() != null
            ? coordinates.getTerritory().getId()
            : null;
    return ChildDataRequest.builder()
        .appId(appId)
        .terId(terId)
        .taskId(task.getId())
        .parameters(parameters)
        .principalKind(resolvePrincipalKind(adminGodMode))
        .coordinates(coordinates)
        .task(task)
        .scope(scope)
        .build();
  }

  private PrincipalKind resolvePrincipalKind(boolean adminGodMode) {
    if (adminGodMode && currentUserHasRole("ROLE_ADMIN")) {
      return PrincipalKind.ADMIN;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication != null ? authentication.getName() : null;
    if (currentUserHasRole("ROLE_PUBLIC") || SecurityConstants.isPublicPrincipal(username)) {
      return PrincipalKind.PUBLIC;
    }
    return PrincipalKind.USER;
  }

  private TemplateTaskExecutionResponseDto toExecutionResponse(Task task, ChildDataResult result) {
    if (result == null || result.getOutcome() == ChildDataOutcome.NO_DATA) {
      return buildNoDataExecutionResponse(task);
    }
    if (result.getOutcome() == ChildDataOutcome.UNSUPPORTED) {
      Object unsupportedScope =
          result.getContext() != null ? result.getContext().get("scope") : null;
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unsupported linked task scope: " + unsupportedScope);
    }
    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status(COMPLETED)
        .resultType(result.getResultType())
        .context(result.getContext())
        .rows(result.getRows() == null ? Collections.emptyList() : result.getRows())
        .resourceUrl(result.getResourceUrl())
        .build();
  }

  private TemplateTaskExecutionResponseDto executeTemplateTask(
      Task task,
      Map<String, String> templateParameters,
      Map<String, Map<String, Object>> childTaskParameters,
      Integer rootTemplateTaskId,
      RequestCoordinates coordinates,
      int depth,
      boolean isolateTemplateChildFailures,
      boolean adminGodMode) {
    List<TaskRelation> relations = taskRelationRepository.findByTaskId(task.getId());
    Map<String, Object> templateContext = new LinkedHashMap<>();

    readTemplateDefaultParameters(task)
        .forEach((key, value) -> templateContext.put("$" + key, value));

    if (templateParameters != null) {
      templateParameters.forEach((key, value) -> templateContext.put("$" + key, value));
    }

    for (TaskRelation relation : relations) {
      if (!List.of("template-task", "template-nested").contains(relation.getRelationType())) {
        continue;
      }

      Task childTask = relation.getRelatedTask();
      Map<String, Object> rawParams =
          childTaskParameters.getOrDefault(
              String.valueOf(childTask.getId()), Collections.emptyMap());
      String referenceAlias = resolveReferenceAlias(relation);
      Map<String, Object> childContext;
      if (!mayAccessTask(childTask, coordinates, adminGodMode)) {
        childContext = buildChildNoDataContext();
        templateContext.put(referenceAlias, childContext);
        templateContext.put(buildLegacyReferenceAlias(childTask), childContext);
        continue;
      }
      try {
        TemplateTaskExecutionResponseDto childResult =
            executeTask(
                childTask,
                stringifyParameters(rawParams),
                childTaskParameters,
                rootTemplateTaskId != null ? rootTemplateTaskId : task.getId(),
                coordinates,
                depth,
                isolateTemplateChildFailures,
                adminGodMode);
        childContext = childResult.getContext();
      } catch (ResponseStatusException exception) {
        if (!isolateTemplateChildFailures || isTemplateNestingDepthExceeded(exception)) {
          throw exception;
        }
        log.warn(
            "Template child task {} failed while rendering template {}",
            childTask.getId(),
            task.getId(),
            exception);
        childContext = buildChildErrorContext(childTask, exception);
      }
      templateContext.put(referenceAlias, childContext);
      templateContext.put(buildLegacyReferenceAlias(childTask), childContext);
    }

    TemplatePreviewResponseDto rendered =
        renderTemplatePreview(
            readTemplateHtml(task),
            templateContext,
            rootTemplateTaskId != null ? rootTemplateTaskId : task.getId());

    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status(COMPLETED)
        .resultType(TEMPLATE)
        .context(Collections.singletonMap("html", rendered.getHtml()))
        .rows(Collections.emptyList())
        .resourceUrl(null)
        .build();
  }

  private TemplatePreviewResponseDto renderTemplatePreview(
      String templateHtml, Map<String, Object> templateContext, Integer templateTaskId) {
    String language = resolveRequestLanguage();
    if (StringUtils.hasText(language)) {
      return templateRenderService.renderPreview(
          templateHtml, templateContext, templateTaskId, Collections.emptyList(), language);
    }
    return templateRenderService.renderPreview(templateHtml, templateContext, templateTaskId);
  }

  private Map<String, Object> buildChildErrorContext(
      Task childTask, ResponseStatusException exception) {
    Map<String, Object> context = new LinkedHashMap<>();
    String taskName =
        childTask.getName() != null ? childTask.getName() : String.valueOf(childTask.getId());
    String message =
        StringUtils.hasText(exception.getReason()) ? exception.getReason() : exception.getMessage();
    String safeMessage =
        escapeHtml(StringUtils.hasText(message) ? message : "Error ejecutando tarea");
    context.put("taskId", childTask.getId());
    context.put("status", "ERROR");
    context.put("statusCode", exception.getStatusCode().value());
    context.put("error", true);
    context.put("message", safeMessage);
    context.put(
        "html",
        "<div class=\"sitmun-template-child-error\">Error ejecutando tarea: "
            + escapeHtml(taskName)
            + " - "
            + safeMessage
            + DIV_CLOSING_TAG);
    context.put(VALUE, "[error: " + safeMessage + "]");
    return context;
  }

  private Map<String, Object> buildChildNoDataContext() {
    String noDataMessage = resolveNoDataMessage();
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("status", "NO_DATA");
    context.put("statusCode", HttpStatus.FORBIDDEN.value());
    context.put("error", false);
    context.put("message", noDataMessage);
    context.put("html", renderNoDataHtml());
    context.put(VALUE, noDataMessage);
    context.put("rows", Collections.emptyList());
    return context;
  }

  private boolean isTemplateNestingDepthExceeded(ResponseStatusException exception) {
    return HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())
        && StringUtils.hasText(exception.getReason())
        && exception.getReason().startsWith(TEMPLATE_NESTING_DEPTH_EXCEEDED_PREFIX);
  }

  private Map<String, String> stringifyParameters(Map<String, Object> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return new LinkedHashMap<>();
    }
    Map<String, String> normalized = new LinkedHashMap<>();
    parameters.forEach(
        (key, value) -> normalized.put(key, value == null ? "" : String.valueOf(value)));
    return normalized;
  }

  private String resolveReferenceAlias(TaskRelation relation) {
    String referenceAlias = relation.getReferenceAlias();
    if (StringUtils.hasText(referenceAlias)
        && REFERENCE_ALIAS_PATTERN.matcher(referenceAlias.trim()).matches()) {
      return referenceAlias.trim();
    }
    return buildLegacyReferenceAlias(relation.getRelatedTask());
  }

  private String buildLegacyReferenceAlias(Task relatedTask) {
    return "task_" + relatedTask.getId();
  }

  private String readTemplateHtml(Task task) {
    Object raw = task.getProperties().get(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML);
    return raw != null ? String.valueOf(raw) : "";
  }
}
