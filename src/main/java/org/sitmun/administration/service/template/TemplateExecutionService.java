package org.sitmun.administration.service.template;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderResponseDto;
import org.sitmun.administration.controller.dto.MapImageRenderRequestDto;
import org.sitmun.administration.controller.dto.MoreInfoAdvancedRenderedTaskDto;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionRequestDto;
import org.sitmun.administration.controller.dto.TemplateTaskExecutionResponseDto;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.database.tester.DatabaseSQLException;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.administration.service.mapimage.MapImageTaskExecutionService;
import org.sitmun.authorization.client.service.AuthorizationService;
import org.sitmun.authorization.proxy.dto.ConfigProxyDto;
import org.sitmun.authorization.proxy.dto.ConfigProxyRequestDto;
import org.sitmun.authorization.proxy.dto.HttpSecurityDto;
import org.sitmun.authorization.proxy.exception.BadRequestException;
import org.sitmun.authorization.proxy.protocols.jdbc.JdbcPayloadDto;
import org.sitmun.authorization.proxy.protocols.wms.WmsPayloadDto;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.database.DatabaseConnection;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.task.relation.TaskRelationRepository;
import org.sitmun.infrastructure.security.core.SecurityRole;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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

  private static final String BINARY_VALUE_PLACEHOLDER = "[contenido binario]";
  private static final int MAX_TEMPLATE_NESTING_LEVEL = 3;
  private static final String TEMPLATE_NESTING_DEPTH_EXCEEDED_PREFIX = "Template nesting depth exceeded";
  private static final String NO_DATA_LITERAL = "Sense dades";
  private static final double MAP_IMAGE_PROJECTED_MIN_DEGENERATE_BBOX_SIZE = 150d;
  private static final double MAP_IMAGE_GEOGRAPHIC_MIN_DEGENERATE_BBOX_SIZE = 0.0015d;
  private static final int DEFAULT_MAP_IMAGE_WIDTH = 1024;
  private static final int DEFAULT_MAP_IMAGE_HEIGHT = 768;
  private static final Pattern REFERENCE_ALIAS_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
  private static final Pattern URI_TEMPLATE_PARAMETER_PATTERN = Pattern.compile("\\{([^/{}]+)}");
  private static final TypeReference<List<Object>> ARRAY_TYPE_REFERENCE = new TypeReference<>() {
  };
  private static final TypeReference<Map<String, Object>> OBJECT_TYPE_REFERENCE = new TypeReference<>() {
  };
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
  private static final List<String> MAP_IMAGE_FEATURE_BBOX_PARAMETER_KEYS = List.of(
      "featureBboxMinX",
      "featureBboxMinY",
      "featureBboxMaxX",
      "featureBboxMaxY");

  private final TaskRepository taskRepository;
  private final TaskRelationRepository taskRelationRepository;
  private final ProxyConfigurationService proxyConfigurationService;
  private final DatabaseConnectionService databaseConnectionService;
  private final HttpClientFactory httpClientFactory;
  private final MapImageTaskExecutionService mapImageTaskExecutionService;
  private final SystemVariableResolver systemVariableResolver;
  private final TemplateRenderService templateRenderService;
  private final TemplateRequestCoordinatesService templateRequestCoordinatesService;
  private final AuthorizationService authorizationService;

  private final ObjectMapper objectMapper;

  public TemplateTaskExecutionResponseDto executeLinkedTask(
      TemplateTaskExecutionRequestDto requestDto) {
    Task task = taskRepository
        .findById(requestDto.getLinkedTaskId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    Integer rootTemplateTaskId = requestDto.getTemplateTaskId();
    if (rootTemplateTaskId == null
        && task.getType() != null
        && Integer.valueOf(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
            .equals(task.getType().getId())) {
      rootTemplateTaskId = task.getId();
    }

    RequestCoordinates coordinates = templateRequestCoordinatesService.build(rootTemplateTaskId);
    Map<String, Map<String, Object>> childTaskParameters = requestDto.getChildTaskParameters() == null
        ? Collections.emptyMap()
        : requestDto.getChildTaskParameters();

    if (!mayAccessTask(task, coordinates)) {
      return buildNoDataExecutionResponse(task);
    }

    return executeTask(
        task,
        stringifyParameters(requestDto.getParameters()),
        childTaskParameters,
        rootTemplateTaskId,
        coordinates,
        0,
        false);
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

  @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
  public MoreInfoAdvancedRenderResponseDto renderMoreInfoAdvanced(
      MoreInfoAdvancedRenderRequestDto requestDto) {
    List<MoreInfoAdvancedRenderedTaskDto> renderedTasks = new ArrayList<>();
    List<Integer> miaTaskIds = requestDto.getMiaTaskIds() == null ? List.of() : requestDto.getMiaTaskIds();
    Map<String, Object> featureParameters = buildViewerParameters(requestDto);
    Set<Integer> accessibleTaskIds = resolveAccessibleTaskIds(requestDto);
    RequestCoordinates profileCoordinates =
        requestDto.getApplicationId() != null && requestDto.getTerritoryId() != null
            ? templateRequestCoordinatesService.buildForProfile(
                requestDto.getApplicationId(), requestDto.getTerritoryId())
            : null;

    for (Integer miaTaskId : miaTaskIds) {
      if (accessibleTaskIds != null && !accessibleTaskIds.contains(miaTaskId)) {
        throw new AccessDeniedException("Access denied to task " + miaTaskId);
      }
      renderedTasks.add(
          renderSingleMoreInfoAdvancedTask(
              miaTaskId, featureParameters, profileCoordinates, accessibleTaskIds));
    }

    return MoreInfoAdvancedRenderResponseDto.builder().tasks(renderedTasks).build();
  }

  private Set<Integer> resolveAccessibleTaskIds(MoreInfoAdvancedRenderRequestDto requestDto) {
    if (SecurityRole.isAdmin() || !currentUserIsAuthenticated()) {
      return null;
    }
    if (requestDto.getApplicationId() == null || requestDto.getTerritoryId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "applicationId and territoryId are required");
    }
    String username = resolveAuthorizedUsername(null);
    if (!StringUtils.hasText(username)) {
      throw new AccessDeniedException("Authenticated user is required");
    }
    return authorizationService
        .findTasksByUserApplicationAndTerritory(
            username, requestDto.getApplicationId(), requestDto.getTerritoryId())
        .stream()
        .map(Task::getId)
        .collect(Collectors.toSet());
  }

  private MoreInfoAdvancedRenderedTaskDto renderSingleMoreInfoAdvancedTask(
      Integer miaTaskId,
      Map<String, Object> featureParameters,
      RequestCoordinates requestProfileCoordinates,
      Set<Integer> accessibleTaskIds) {
    Task miaTask = taskRepository
        .findById(miaTaskId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

    if (!DomainConstants.Tasks.isMoreInfoAdvancedTask(miaTask)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is not a MIA task");
    }

    Map<String, Object> properties = miaTask.getProperties() == null ? Collections.emptyMap() : miaTask.getProperties();
    Map<String, Object> miaParameters = convertBasicParameters(properties);
    String visualizationMode = SCROLL.equals(miaParameters.get("visualizationMode"))
        || SCROLL.equals(properties.get("parentLayout"))
            ? SCROLL
            : "tabs";
    List<Map<String, Object>> includedTasks = readIncludedTasks(miaTask, miaParameters);
    List<Map<String, Object>> renderableTasks = filterRenderableMiaChildren(includedTasks);
    RequestCoordinates coordinates =
        requestProfileCoordinates != null
            ? requestProfileCoordinates
            : templateRequestCoordinatesService.build(miaTask.getId());
    MiaRenderContext renderContext =
        new MiaRenderContext(featureParameters, coordinates, accessibleTaskIds);

    String html = "tabs".equals(visualizationMode)
        ? renderMiaChildrenAsTabs(miaTask, renderableTasks, renderContext)
        : renderMiaChildrenAsScroll(renderableTasks, renderContext);

    return MoreInfoAdvancedRenderedTaskDto.builder()
        .taskId(miaTask.getId())
        .title(miaTask.getName())
        .html(html)
        .build();
  }

  private String renderMiaChildrenAsTabs(
      Task miaTask,
      List<Map<String, Object>> includedTasks,
      MiaRenderContext renderContext) {
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
          .append(
              renderMiaChild(childDefinition, renderContext))
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
      List<Map<String, Object>> includedTasks, MiaRenderContext renderContext) {
    StringBuilder sections = new StringBuilder();
    for (int index = 0; index < includedTasks.size(); index++) {
      Map<String, Object> childDefinition = includedTasks.get(index);
      sections
          .append(
              "<div class=\"sitmun-mia-scroll-section\"><div class=\"sitmun-mia-section-title\">")
          .append(escapeHtml(resolveChildTitle(childDefinition, index)))
          .append(DIV_CLOSING_TAG)
          .append(
              renderMiaChild(childDefinition, renderContext))
          .append(DIV_CLOSING_TAG);
    }
    return "<div class=\"sitmun-mia-body sitmun-mia-scroll-body\">" + sections + DIV_CLOSING_TAG;
  }

  @SuppressWarnings("unchecked")
  private String renderMiaChild(Map<String, Object> childDefinition, MiaRenderContext renderContext) {
    Integer childTaskId = parseTaskId(childDefinition.get("id"));
    if (childTaskId == null) {
      return "<div class=\"sitmun-mia-error\">Invalid child task id</div>";
    }

    Task childTask = taskRepository
        .findById(childTaskId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if ((renderContext.accessibleTaskIds() != null
            && !renderContext.accessibleTaskIds().contains(childTaskId))
        || (renderContext.accessibleTaskIds() == null
            && !mayAccessTask(childTask, renderContext.coordinates()))) {
      return renderNoDataHtml();
    }
    Map<String, Object> resolvedChildParameters = resolveMappedParameters(
        childDefinition.get(PARAMETERS), renderContext.featureParameters());
    if (resolvedChildParameters.isEmpty()) {
      resolvedChildParameters =
          resolveMappedParameters(
              readMiaChildParameterMappings(childTask), renderContext.featureParameters());
    }
    enrichMapImageViewerContextParameters(
        childTask, resolvedChildParameters, renderContext.featureParameters());
    Map<String, String> childParameters = stringifyParameters(resolvedChildParameters);
    Map<String, Map<String, Object>> childTaskParameters = new LinkedHashMap<>(
        resolveMappedChildTaskParameters(
            childDefinition.get(CHILD_TASK_PARAMETERS), renderContext.featureParameters()));
    Integer rootTemplateTaskId = childTask.getType() != null
        && Integer.valueOf(DomainConstants.Tasks.TASK_TYPE_ID_TEMPLATE)
            .equals(childTask.getType().getId())
                ? childTask.getId()
                : null;
    if (rootTemplateTaskId != null) {
      mergeMappedTemplateChildTaskParameters(
          childTaskParameters,
          childDefinition.get(TEMPLATE_CHILD_TASK_PARAMETERS),
          rootTemplateTaskId,
          renderContext.featureParameters());
      enrichTemplateChildTaskParameters(
          childTask, childTaskParameters, renderContext.featureParameters(), 0);
    }
    TemplateTaskExecutionResponseDto result;
    boolean isTemplateChild = rootTemplateTaskId != null;
    try {
      result = executeTask(
          childTask,
          childParameters,
          childTaskParameters,
        rootTemplateTaskId,
        renderContext.coordinates(),
          0,
          isTemplateChild);
    } catch (ResponseStatusException exception) {
      if (isTemplateChild || isTemplateNestingDepthExceeded(exception)) {
        throw exception;
      }
      String taskName = childTask.getName() != null ? childTask.getName() : String.valueOf(childTaskId);
      return "<div class=\"sitmun-mia-error\">Error ejecutando tarea: "
          + escapeHtml(taskName)
          + DIV_CLOSING_TAG;
    }

    if (TEMPLATE.equals(result.getResultType())) {
      Object html = result.getContext() != null ? result.getContext().get("html") : null;
      String content = html == null ? "" : String.valueOf(html);
      return wrapWithDownloadAnnotation(content, childTaskId);
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
      Map<String, Object> resolvedParameters = resolveMappedParameters(readMiaChildParameterMappings(relatedTask),
          featureParameters);
      enrichMapImageViewerContextParameters(relatedTask, resolvedParameters, featureParameters);
      if (!resolvedParameters.isEmpty()) {
        Map<String, Object> existingParameters = childTaskParameters.computeIfAbsent(
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
    Object rawInnerTaskParameters = selectTemplateChildTaskParameters(rawTemplateChildTaskParameters, templateTaskId);
    Map<String, Map<String, Object>> resolvedParameters = resolveMappedChildTaskParameters(rawInnerTaskParameters,
        featureParameters);
    resolvedParameters.forEach(
        (taskId, parameters) -> {
          Map<String, Object> existingParameters = childTaskParameters.computeIfAbsent(taskId,
              ignored -> new LinkedHashMap<>());
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

  private List<Map<String, Object>> filterRenderableMiaChildren(List<Map<String, Object>> includedTasks) {
    return includedTasks.stream()
        .filter(childDefinition -> !"documentExport".equals(resolveMiaChildType(childDefinition)))
        .toList();
  }

  private String resolveMiaChildType(Map<String, Object> childDefinition) {
    Object explicitChildType = childDefinition.get("childType");
    if (explicitChildType != null) {
      String childType = String.valueOf(explicitChildType);
      if ("documentExport".equals(childType)) {
        return childType;
      }
      if ("template".equals(childType)) {
        return childType;
      }
    }

    Integer childTaskId = parseTaskId(childDefinition.get("id"));
    if (childTaskId == null) {
      return "query";
    }

    return taskRepository
        .findById(childTaskId)
        .map(
            childTask -> {
              if (DomainConstants.Tasks.isDocumentExportTask(childTask)) {
                return "documentExport";
              }
              if (isTemplateTask(childTask)) {
                return "template";
              }
              return "query";
            })
        .orElse("query");
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
        (left, right) -> Integer.compare(toInt(left.get(ORDER), 999), toInt(right.get(ORDER), 999)));
    attachTemplateChildTaskParameters(miaTask, miaParameters, includedTasks);
    return includedTasks;
  }

  private void attachTemplateChildTaskParameters(
      Task miaTask, Map<String, Object> miaParameters, List<Map<String, Object>> includedTasks) {
    Map<String, Object> properties = miaTask.getProperties() == null ? Collections.emptyMap() : miaTask.getProperties();
    Object rawTemplateChildTaskParameters = miaParameters.containsKey(TEMPLATE_CHILD_TASK_PARAMETERS)
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
    Map<String, Object> properties = miaTask.getProperties() == null ? Collections.emptyMap() : miaTask.getProperties();
    Object rawChildOrder = properties.get("childTaskOrderIds");
    if (!(rawChildOrder instanceof List<?> childOrder) || childOrder.isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, Object> adminChildTaskParameters = properties.get(CHILD_TASK_PARAMETERS) instanceof Map<?, ?> ctp
        ? (Map<String, Object>) ctp
        : Collections.emptyMap();
    Map<String, Object> adminTemplateChildTaskParameters = properties
        .get(TEMPLATE_CHILD_TASK_PARAMETERS) instanceof Map<?, ?> tctp
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
                Object explicitMapping = getMapValueByTaskId(adminChildTaskParameters, childTask.getId());
                if (explicitMapping instanceof Map<?, ?>
                    && !((Map<?, ?>) explicitMapping).isEmpty()) {
                  childDefinition.put(PARAMETERS, explicitMapping);
                } else {
                  childDefinition.put(PARAMETERS, readMiaChildParameterMappings(childTask));
                }
                Object templateChildTaskMapping = getMapValueByTaskId(adminTemplateChildTaskParameters,
                    childTask.getId());
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
    Map<String, Object> properties = childTask.getProperties() == null ? Collections.emptyMap()
        : childTask.getProperties();
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

  Map<String, Object> buildViewerParameters(MoreInfoAdvancedRenderRequestDto requestDto) {
    Map<String, Object> viewerParameters = new LinkedHashMap<>();
    if (requestDto == null) {
      return viewerParameters;
    }

    if (requestDto.getParameters() != null) {
      viewerParameters.putAll(requestDto.getParameters());
    }

    if (requestDto.getFeatureBbox() != null && requestDto.getFeatureBbox().size() >= 4) {
      List<Double> featureBbox = requestDto.getFeatureBbox();
      viewerParameters.put("featureBboxMinX", featureBbox.get(0));
      viewerParameters.put("featureBboxMinY", featureBbox.get(1));
      viewerParameters.put("featureBboxMaxX", featureBbox.get(2));
      viewerParameters.put("featureBboxMaxY", featureBbox.get(3));
    }

    return viewerParameters;
  }

  private Map<String, Object> readTemplateDefaultParameters(Task task) {
    Map<String, Object> properties = task.getProperties() == null ? Collections.emptyMap() : task.getProperties();
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
      Object convertedValue = rawType == null
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

  private void enrichMapImageViewerContextParameters(
      Task task,
      Map<String, Object> parameters,
      Map<String, Object> featureParameters) {
    if (!DomainConstants.Tasks.isMapImageTask(task)) {
      return;
    }
    MAP_IMAGE_FEATURE_BBOX_PARAMETER_KEYS.forEach(
        key -> {
          Object value = featureParameters.get(key);
          if (value != null) {
            parameters.putIfAbsent(key, value);
          }
        });
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
          Map<String, Object> taskParameters = resolveMappedParameters(rawParameterDefinitions, featureParameters);
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
      Object fieldPath = definition.get(VALUE) != null ? definition.get(VALUE) : definition.get("name");
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
          column -> html.append("<td>")
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

  /**
   * Wraps rendered template HTML with a marker that lets the viewer inject one
   * button per
   * authorized {@code documentExport} task.
   */
  private String wrapWithDownloadAnnotation(String content, Integer templateTaskId) {
    String taskIdAttribute = templateTaskId == null ? "" : " data-mia-template-task-id=\"" + templateTaskId + "\"";
    return "<div data-mia-export-template=\"true\"" + taskIdAttribute + ">" + content + "</div>";
  }

  private boolean mayAccessTask(Task task, RequestCoordinates coordinates) {
    if (task == null || SecurityRole.isAdmin()) {
      return true;
    }
    if (coordinates == null) {
      return true;
    }
    if (!currentUserIsAuthenticated()) {
      return true;
    }
    String username = resolveAuthorizedUsername(coordinates);
    Integer applicationId = coordinates.getApplication() != null ? coordinates.getApplication().getId() : null;
    Integer territoryId = coordinates.getTerritory() != null ? coordinates.getTerritory().getId() : null;
    if (!StringUtils.hasText(username) || applicationId == null || territoryId == null) {
      return false;
    }

    return authorizationService
        .findTasksByUserApplicationAndTerritory(username, applicationId, territoryId)
        .stream()
        .anyMatch(accessibleTask -> task.getId().equals(accessibleTask.getId()));
  }

  private boolean currentUserIsAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && StringUtils.hasText(authentication.getName())
        && authentication.getAuthorities().stream()
            .anyMatch(
                authority ->
                    SecurityRole.USER.authority().equals(authority.getAuthority())
                        || SecurityRole.ADMIN.authority().equals(authority.getAuthority())
                        || SecurityRole.PUBLIC.authority().equals(authority.getAuthority()));
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
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
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
      boolean isolateTemplateChildFailures) {
    boolean isTemplateTask = task.getType() != null
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
          isolateTemplateChildFailures);
    }

    if (DomainConstants.Tasks.isMapImageTask(task)) {
      return executeMapImageTask(task, parameters);
    }

    String scope = String.valueOf(task.getProperties().get(DomainConstants.Tasks.PROPERTY_SCOPE));
    if (DomainConstants.Tasks.SCOPE_SQL_QUERY.equalsIgnoreCase(scope)) {
      return executeSqlTask(task, parameters, coordinates);
    }
    if (DomainConstants.Tasks.SCOPE_WEB_API_QUERY.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_WEB_API_QUERY_NO_PROXY.equalsIgnoreCase(scope)) {
      return executeApiTask(task, parameters, coordinates);
    }
    if (DomainConstants.Tasks.SCOPE_URL_QUERY.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_RESOURCE_QUERY.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_URL.equalsIgnoreCase(scope)
        || DomainConstants.Tasks.SCOPE_RESOURCE.equalsIgnoreCase(scope)) {
      return resolveUrlTask(task, parameters, scope, coordinates);
    }

    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Unsupported linked task scope: " + scope);
  }

  private TemplateTaskExecutionResponseDto executeTemplateTask(
      Task task,
      Map<String, String> templateParameters,
      Map<String, Map<String, Object>> childTaskParameters,
      Integer rootTemplateTaskId,
      RequestCoordinates coordinates,
      int depth,
      boolean isolateTemplateChildFailures) {
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
      Map<String, Object> rawParams = childTaskParameters.getOrDefault(
          String.valueOf(childTask.getId()), Collections.emptyMap());
      String referenceAlias = resolveReferenceAlias(relation);
      Map<String, Object> childContext;
      if (!mayAccessTask(childTask, coordinates)) {
        childContext = buildChildNoDataContext();
        templateContext.put(referenceAlias, childContext);
        templateContext.put(buildLegacyReferenceAlias(childTask), childContext);
        continue;
      }
      try {
        TemplateTaskExecutionResponseDto childResult = executeTask(
            childTask,
            stringifyParameters(rawParams),
            childTaskParameters,
            rootTemplateTaskId != null ? rootTemplateTaskId : task.getId(),
            coordinates,
            depth,
            isolateTemplateChildFailures);
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
    String taskName = childTask.getName() != null ? childTask.getName() : String.valueOf(childTask.getId());
    String message = StringUtils.hasText(exception.getReason()) ? exception.getReason() : exception.getMessage();
    String safeMessage = escapeHtml(StringUtils.hasText(message) ? message : "Error ejecutando tarea");
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

  private TemplateTaskExecutionResponseDto executeSqlTask(
      Task task, Map<String, String> parameters, RequestCoordinates coordinates) {
    ConfigProxyRequestDto configRequest = ConfigProxyRequestDto.builder()
        .appId(coordinates.getApplication() != null ? coordinates.getApplication().getId() : 0)
        .terId(coordinates.getTerritory() != null ? coordinates.getTerritory().getId() : 0)
        .type(DomainConstants.Proxy.TYPE_SQL)
        .typeId(task.getId())
        .parameters(parameters)
        .build();

    ConfigProxyDto config;
    try {
      config = proxyConfigurationService.getConfiguration(configRequest, 0, coordinates);
      proxyConfigurationService.applyDecorators(config, configRequest, coordinates);
    } catch (BadRequestException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }

    JdbcPayloadDto payload = (JdbcPayloadDto) config.getPayload();
    DatabaseConnection connection = DatabaseConnection.builder()
        .driver(payload.getDriver())
        .url(payload.getUri())
        .user(payload.getUser())
        .password(payload.getPassword())
        .build();

    List<Map<String, Object>> rows;
    try {
      rows = databaseConnectionService.executeQuery(
          connection, payload.getSql(), payload.getParameters());
    } catch (DatabaseSQLException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage(),
          exception);
    }

    Map<String, Object> context = buildRowAndParameterContext(rows, parameters);
    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status(COMPLETED)
        .resultType(TABLE)
        .context(context)
        .rows(rows)
        .resourceUrl(null)
        .build();
  }

  private TemplateTaskExecutionResponseDto executeApiTask(
      Task task, Map<String, String> parameters, RequestCoordinates coordinates) {
    ConfigProxyRequestDto configRequest = ConfigProxyRequestDto.builder()
        .appId(coordinates.getApplication() != null ? coordinates.getApplication().getId() : 0)
        .terId(coordinates.getTerritory() != null ? coordinates.getTerritory().getId() : 0)
        .type(DomainConstants.Proxy.TYPE_API)
        .typeId(task.getId())
        .parameters(parameters)
        .build();

    ConfigProxyDto config;
    try {
      config = proxyConfigurationService.getConfiguration(configRequest, 0, coordinates);
      proxyConfigurationService.applyDecorators(config, configRequest, coordinates);
    } catch (BadRequestException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
    }

    WmsPayloadDto payload = (WmsPayloadDto) config.getPayload();
    String requestUrl = buildHttpRequestUrl(payload, parameters, coordinates, readTaskCommand(task));
    String sanitizedRequestUrl = sanitizeRequestUrlForLogging(requestUrl, parameters);

    try {
      Request.Builder requestBuilder = new Request.Builder().url(requestUrl);
      applySecurity(payload.getSecurity(), requestBuilder);

      String method = StringUtils.hasText(payload.getMethod()) ? payload.getMethod().toUpperCase() : "GET";
      if ("POST".equals(method)) {
        RequestBody postBody = RequestBody.create(
            payload.getBody() == null ? "" : payload.getBody(),
            okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE));
        requestBuilder.post(postBody);
      } else {
        requestBuilder.get();
      }

      log.info(
          "Executing template API task {} with method {} at {}",
          task.getId(),
          method,
          sanitizedRequestUrl);

      try (Response response = httpClientFactory.executeRequest(requestBuilder.build())) {
        okhttp3.ResponseBody responseBody = response.body();
        okhttp3.MediaType contentType = responseBody != null ? responseBody.contentType() : null;
        String resolvedMimeType = resolveApiResponseMimeType(task, contentType);
        if (response.isSuccessful() && isBinaryMimeType(resolvedMimeType)) {
          log.info(
              "Template API task {} returned HTTP {} binary content-type {} length {}",
              task.getId(),
              response.code(),
              resolvedMimeType,
              responseBody != null ? responseBody.contentLength() : 0);
          return buildBinaryApiResponse(
              task,
              parameters,
              payload.getParameters(),
              hasServerSideHttpSecurity(payload.getSecurity()) ? null : requestUrl,
              resolvedMimeType);
        }
        String body = responseBody != null ? responseBody.string() : "";
        log.info(
            "Template API task {} returned HTTP {} content-type {} body length {}",
            task.getId(),
            response.code(),
            contentType,
            body.length());
        if (!response.isSuccessful()) {
          log.warn(
              "Template API task {} failed with HTTP {} body length {}",
              task.getId(),
              response.code(),
              body.length());
          throw new ResponseStatusException(
              HttpStatus.BAD_GATEWAY, "API task returned HTTP " + response.code());
        }
        Map<String, Object> bodyContext = normalizeBodyToContext(body);
        List<Map<String, Object>> rows = flattenContextToRows(bodyContext);
        Map<String, Object> context = buildApiContext(bodyContext, rows, payload.getParameters(), parameters);

        return TemplateTaskExecutionResponseDto.builder()
            .taskId(task.getId())
            .status(COMPLETED)
            .resultType(TABLE)
            .context(context)
            .rows(rows)
            .resourceUrl(null)
            .build();
      }
    } catch (IOException e) {
      log.warn(
          "Template API task {} failed while calling {}", task.getId(), sanitizedRequestUrl, e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to execute API task", e);
    }
  }

  private TemplateTaskExecutionResponseDto executeMapImageTask(Task task, Map<String, String> parameters) {
    MapImageRenderRequestDto requestDto = new MapImageRenderRequestDto();
    requestDto.setTaskId(task.getId());
    List<Double> featureDrivenBbox = resolveFeatureDrivenMapImageBbox(task, parameters);
    if (featureDrivenBbox != null) {
      requestDto.setBbox(featureDrivenBbox);
    }

    byte[] content = mapImageTaskExecutionService.renderMapImage(requestDto);
    String contentUrl = "data:" + MediaType.IMAGE_PNG_VALUE + ";base64," + Base64.getEncoder().encodeToString(content);
    return buildMapImageBinaryResponse(task, contentUrl);
  }

  private List<Double> resolveFeatureDrivenMapImageBbox(Task task, Map<String, String> parameters) {
    List<Double> featureBbox = readFeatureBbox(parameters);
    if (featureBbox == null) {
      return null;
    }

    List<Double> expandedBbox = expandDegenerateBbox(featureBbox, readMapImageDegenerateBboxSize(task));
    List<Double> paddedBbox = applyBboxMargin(expandedBbox, readMapImageBboxMarginRatio(task));
    return fitBboxToAspectRatio(
        paddedBbox,
        readPositiveTaskDimension(task, DomainConstants.Tasks.PROPERTY_WIDTH, DEFAULT_MAP_IMAGE_WIDTH),
        readPositiveTaskDimension(task, DomainConstants.Tasks.PROPERTY_HEIGHT, DEFAULT_MAP_IMAGE_HEIGHT));
  }

  private double readMapImageBboxMarginRatio(Task task) {
    if (task == null || task.getProperties() == null) {
      return 0d;
    }
    Object rawValue = task.getProperties().get(DomainConstants.Tasks.PROPERTY_BBOX_MARGIN_PERCENT);
    if (!(rawValue instanceof Number number)) {
      return 0d;
    }
    return Math.max(0d, number.doubleValue()) / 100d;
  }

  private double readMapImageDegenerateBboxSize(Task task) {
    if (task == null || task.getProperties() == null) {
      return MAP_IMAGE_PROJECTED_MIN_DEGENERATE_BBOX_SIZE;
    }
    Object rawSrs = task.getProperties().get(DomainConstants.Tasks.PROPERTY_SRS);
    String srs = rawSrs instanceof String value ? value.trim() : "";
    return isGeographicSrs(srs)
        ? MAP_IMAGE_GEOGRAPHIC_MIN_DEGENERATE_BBOX_SIZE
        : MAP_IMAGE_PROJECTED_MIN_DEGENERATE_BBOX_SIZE;
  }

  private boolean isGeographicSrs(String srs) {
    return "EPSG:4326".equalsIgnoreCase(srs) || "CRS:84".equalsIgnoreCase(srs);
  }

  private List<Double> readFeatureBbox(Map<String, String> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return null;
    }

    boolean hasFeatureBboxParameter = parameters.containsKey("featureBboxMinX")
        || parameters.containsKey("featureBboxMinY")
        || parameters.containsKey("featureBboxMaxX")
        || parameters.containsKey("featureBboxMaxY");
    if (!hasFeatureBboxParameter) {
      return null;
    }

    validateRequiredFeatureBboxParameters(parameters);

    Double minX = readDoubleParameter(parameters, "featureBboxMinX");
    Double minY = readDoubleParameter(parameters, "featureBboxMinY");
    Double maxX = readDoubleParameter(parameters, "featureBboxMaxX");
    Double maxY = readDoubleParameter(parameters, "featureBboxMaxY");
    return List.of(minX, minY, maxX, maxY);
  }

  private void validateRequiredFeatureBboxParameters(Map<String, String> parameters) {
    List<String> requiredKeys = List.of(
        "featureBboxMinX",
        "featureBboxMinY",
        "featureBboxMaxX",
        "featureBboxMaxY");
    boolean missingParameter = requiredKeys.stream()
        .anyMatch((key) -> !StringUtils.hasText(parameters.get(key)));
    if (missingParameter) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "featureBbox parameters must include featureBboxMinX, featureBboxMinY, featureBboxMaxX and featureBboxMaxY together");
    }
  }

  private Double readDoubleParameter(Map<String, String> parameters, String key) {
    String rawValue = parameters.get(key);
    if (!StringUtils.hasText(rawValue)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "featureBbox parameter " + key + " must be numeric");
    }
    try {
      return Double.valueOf(rawValue.trim());
    } catch (NumberFormatException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "featureBbox parameter " + key + " must be numeric",
          exception);
    }
  }

  private List<Double> expandDegenerateBbox(List<Double> bbox, double minDegenerateBboxSize) {
    double minX = bbox.get(0);
    double minY = bbox.get(1);
    double maxX = bbox.get(2);
    double maxY = bbox.get(3);

    if (Double.compare(minX, maxX) == 0) {
      minX -= minDegenerateBboxSize / 2d;
      maxX += minDegenerateBboxSize / 2d;
    }
    if (Double.compare(minY, maxY) == 0) {
      minY -= minDegenerateBboxSize / 2d;
      maxY += minDegenerateBboxSize / 2d;
    }

    return List.of(minX, minY, maxX, maxY);
  }

  private List<Double> applyBboxMargin(List<Double> bbox, double marginRatio) {
    double width = bbox.get(2) - bbox.get(0);
    double height = bbox.get(3) - bbox.get(1);
    double horizontalMargin = width * marginRatio / 2d;
    double verticalMargin = height * marginRatio / 2d;
    return List.of(
        bbox.get(0) - horizontalMargin,
        bbox.get(1) - verticalMargin,
        bbox.get(2) + horizontalMargin,
        bbox.get(3) + verticalMargin);
  }

  private List<Double> fitBboxToAspectRatio(List<Double> bbox, int width, int height) {
    double minX = bbox.get(0);
    double minY = bbox.get(1);
    double maxX = bbox.get(2);
    double maxY = bbox.get(3);
    double bboxWidth = maxX - minX;
    double bboxHeight = maxY - minY;
    if (bboxWidth <= 0d || bboxHeight <= 0d || width <= 0 || height <= 0) {
      return bbox;
    }

    double bboxCenterX = (minX + maxX) / 2d;
    double bboxCenterY = (minY + maxY) / 2d;
    double bboxRatio = bboxWidth / bboxHeight;
    double targetRatio = (double) width / (double) height;

    if (bboxRatio < targetRatio) {
      double fittedWidth = bboxHeight * targetRatio;
      double halfWidth = fittedWidth / 2d;
      return List.of(bboxCenterX - halfWidth, minY, bboxCenterX + halfWidth, maxY);
    }

    if (bboxRatio > targetRatio) {
      double fittedHeight = bboxWidth / targetRatio;
      double halfHeight = fittedHeight / 2d;
      return List.of(minX, bboxCenterY - halfHeight, maxX, bboxCenterY + halfHeight);
    }

    return bbox;
  }

  private int readPositiveTaskDimension(Task task, String propertyKey, int defaultValue) {
    if (task == null || task.getProperties() == null) {
      return defaultValue;
    }
    Object rawValue = task.getProperties().get(propertyKey);
    return rawValue instanceof Number number && number.intValue() > 0 ? number.intValue() : defaultValue;
  }

  private TemplateTaskExecutionResponseDto buildMapImageBinaryResponse(Task task, String contentUrl) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("contentUrl", contentUrl);
    context.put("url", contentUrl);
    context.put("mimeType", MediaType.IMAGE_PNG_VALUE);
    context.put("binary", true);
    context.put("embeddable", true);
    context.put(VALUE, BINARY_VALUE_PLACEHOLDER);

    List<Map<String, Object>> rows = flattenContextToRows(context);
    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status(COMPLETED)
        .resultType("resource")
        .context(context)
        .rows(rows)
        .resourceUrl(contentUrl)
        .build();
  }

  private String sanitizeRequestUrlForLogging(
      String requestUrl, Map<String, String> executionParameters) {
    if (!StringUtils.hasText(requestUrl)) {
      return "";
    }
    return maskQueryParameterValues(maskExecutionParameterValues(requestUrl, executionParameters));
  }

  private String maskExecutionParameterValues(
      String requestUrl, Map<String, String> executionParameters) {
    if (executionParameters == null || executionParameters.isEmpty()) {
      return requestUrl;
    }
    String sanitized = requestUrl;
    List<Map.Entry<String, String>> entries = new ArrayList<>(executionParameters.entrySet());
    entries.sort(Comparator.comparingInt(entry -> -String.valueOf(entry.getValue()).length()));
    for (Map.Entry<String, String> entry : entries) {
      if (!StringUtils.hasText(entry.getKey()) || !StringUtils.hasText(entry.getValue())) {
        continue;
      }
      String placeholder = "{" + entry.getKey() + "}";
      String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
      sanitized = sanitized.replace(encodedValue.replace("+", "%20"), placeholder);
      sanitized = sanitized.replace(encodedValue, placeholder);
      sanitized = sanitized.replace(entry.getValue(), placeholder);
    }
    return sanitized;
  }

  private String maskQueryParameterValues(String requestUrl) {
    int fragmentStart = requestUrl.indexOf('#');
    String urlWithoutFragment = fragmentStart >= 0 ? requestUrl.substring(0, fragmentStart) : requestUrl;
    String fragment = fragmentStart >= 0 ? requestUrl.substring(fragmentStart) : "";
    int queryStart = urlWithoutFragment.indexOf('?');
    if (queryStart < 0) {
      return requestUrl;
    }
    String prefix = urlWithoutFragment.substring(0, queryStart + 1);
    String query = urlWithoutFragment.substring(queryStart + 1);
    if (query.isEmpty()) {
      return requestUrl;
    }
    List<String> maskedParameters = new ArrayList<>();
    for (String queryParameter : query.split("&", -1)) {
      int valueStart = queryParameter.indexOf('=');
      if (valueStart < 0) {
        maskedParameters.add(queryParameter);
      } else {
        maskedParameters.add(queryParameter.substring(0, valueStart + 1) + "***");
      }
    }
    return prefix + String.join("&", maskedParameters) + fragment;
  }

  private TemplateTaskExecutionResponseDto resolveUrlTask(
      Task task, Map<String, String> parameters, String scope, RequestCoordinates coordinates) {
    String command = String.valueOf(
        task.getProperties().getOrDefault(DomainConstants.Tasks.PROPERTY_COMMAND, ""));
    String resolved = resolveTemplateUrl(command, parameters, coordinates);

    Map<String, Object> context = new LinkedHashMap<>();
    context.put("url", resolved);
    parameters.forEach((key, value) -> context.put("$" + key, value));

    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status(COMPLETED)
        .resultType(
            DomainConstants.Tasks.SCOPE_RESOURCE_QUERY.equalsIgnoreCase(scope)
                || DomainConstants.Tasks.SCOPE_RESOURCE.equalsIgnoreCase(scope)
                    ? "resource"
                    : "url")
        .context(context)
        .rows(Collections.emptyList())
        .resourceUrl(resolved)
        .build();
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

  private Map<String, Object> buildRowAndParameterContext(
      List<Map<String, Object>> rows, Map<String, String> parameters) {
    Map<String, Object> context = new LinkedHashMap<>();
    parameters.forEach((key, value) -> context.put("$" + key, value));
    context.put("rows", rows);
    if (!rows.isEmpty()) {
      rows.get(0).forEach(context::put);
    }
    return context;
  }

  private Map<String, Object> buildApiContext(
      Map<String, Object> bodyContext,
      List<Map<String, Object>> rows,
      Map<String, String> configuredParameters,
      Map<String, String> executionParameters) {
    Map<String, Object> context = new LinkedHashMap<>();
    mergeTemplateParameterContext(context, configuredParameters, executionParameters);
    context.put("rows", rows);
    if (bodyContext != null) {
      context.putAll(bodyContext);
    }
    return context;
  }

  private TemplateTaskExecutionResponseDto buildBinaryApiResponse(
      Task task,
      Map<String, String> executionParameters,
      Map<String, String> configuredParameters,
      String contentUrl,
      String mimeType) {
    Map<String, Object> context = new LinkedHashMap<>();
    mergeTemplateParameterContext(context, configuredParameters, executionParameters);
    context.put("contentUrl", contentUrl);
    context.put("url", contentUrl);
    context.put("mimeType", mimeType);
    context.put("binary", true);
    context.put("embeddable", StringUtils.hasText(contentUrl));
    if (!StringUtils.hasText(contentUrl)) {
      context.put(
          "accessMessage", "Contenido binario no embebible: requiere autenticacion de servidor");
    }
    context.put(VALUE, BINARY_VALUE_PLACEHOLDER);

    List<Map<String, Object>> rows = flattenContextToRows(context);
    return TemplateTaskExecutionResponseDto.builder()
        .taskId(task.getId())
        .status(COMPLETED)
        .resultType("resource")
        .context(context)
        .rows(rows)
        .resourceUrl(contentUrl)
        .build();
  }

  private String resolveApiResponseMimeType(Task task, okhttp3.MediaType contentType) {
    String configuredMimeType = readConfiguredMimeType(task);
    if (StringUtils.hasText(configuredMimeType)) {
      return configuredMimeType.trim();
    }
    return contentType != null ? contentType.toString() : null;
  }

  private String readConfiguredMimeType(Task task) {
    Map<String, Object> properties = task.getProperties();
    Object mimeType = properties != null ? properties.get(DomainConstants.Tasks.PROPERTY_MIME_TYPE) : null;
    return mimeType != null ? String.valueOf(mimeType) : null;
  }

  private boolean isBinaryMimeType(String mimeType) {
    if (!StringUtils.hasText(mimeType)) {
      return true;
    }
    String normalized = mimeType.toLowerCase().split(";", 2)[0].trim();
    if (normalized.startsWith("text/")
        || normalized.equals(MediaType.APPLICATION_JSON_VALUE)
        || normalized.equals(MediaType.APPLICATION_XML_VALUE)
        || normalized.equals(MediaType.TEXT_XML_VALUE)
        || normalized.equals("application/xhtml+xml")
        || normalized.equals("application/javascript")
        || normalized.equals("application/x-javascript")
        || normalized.equals("application/ecmascript")
        || normalized.equals("application/x-www-form-urlencoded")
        || normalized.equals("application/csv")
        || normalized.endsWith("+json")
        || normalized.endsWith("+xml")) {
      return false;
    }
    return true;
  }

  private boolean hasServerSideHttpSecurity(HttpSecurityDto security) {
    if (security == null) {
      return false;
    }
    return (security.getHeaders() != null && !security.getHeaders().isEmpty())
        || StringUtils.hasText(security.getUsername())
        || StringUtils.hasText(security.getPassword());
  }

  private void mergeTemplateParameterContext(
      Map<String, Object> context,
      Map<String, String> configuredParameters,
      Map<String, String> executionParameters) {
    if (configuredParameters != null) {
      configuredParameters.forEach((key, value) -> context.put("$" + key, value));
    }
    if (executionParameters != null) {
      executionParameters.forEach((key, value) -> context.put("$" + key, value));
    }
  }

  private Map<String, Object> normalizeBodyToContext(String body) throws IOException {
    if (!StringUtils.hasText(body)) {
      return Collections.emptyMap();
    }
    String trimmed = body.trim();
    if (trimmed.startsWith("[")) {
      List<?> values = objectMapper.readValue(trimmed, new TypeReference<List<?>>() {
      });
      return Collections.singletonMap("items", values);
    }
    if (trimmed.startsWith("{")) {
      return objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() {
      });
    }
    return Collections.singletonMap(VALUE, body);
  }

  private List<Map<String, Object>> flattenContextToRows(Map<String, Object> context) {
    List<Map<String, Object>> rows = new ArrayList<>();
    flattenValue(null, context, rows);
    return rows;
  }

  private void flattenValue(String path, Object value, List<Map<String, Object>> rows) {
    if (value instanceof Map<?, ?> mapValue) {
      mapValue.forEach(
          (key, nestedValue) -> flattenValue(appendPath(path, String.valueOf(key)), nestedValue, rows));
      return;
    }
    if (value instanceof List<?> listValue) {
      for (int index = 0; index < listValue.size(); index++) {
        flattenValue(appendIndex(path, index), listValue.get(index), rows);
      }
      return;
    }
    if (path != null) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("field", path);
      row.put(VALUE, value);
      rows.add(row);
    }
  }

  private String appendPath(String parent, String segment) {
    return parent == null || parent.isEmpty() ? segment : parent + "." + segment;
  }

  private String appendIndex(String parent, int index) {
    return (parent == null ? "" : parent) + "[" + index + "]";
  }

  private void applySecurity(HttpSecurityDto security, Request.Builder requestBuilder) {
    if (security == null) {
      return;
    }
    if (security.getHeaders() != null) {
      security.getHeaders().forEach(requestBuilder::addHeader);
    }
    if (StringUtils.hasText(security.getUsername())
        && StringUtils.hasText(security.getPassword())) {
      requestBuilder.addHeader(
          "Authorization",
          Credentials.basic(
              security.getUsername(), security.getPassword(), StandardCharsets.UTF_8));
    }
  }

  private String buildHttpRequestUrl(
      WmsPayloadDto payload,
      Map<String, String> executionParameters,
      RequestCoordinates coordinates,
      String taskCommand) {
    Map<String, String> templateParameters = new LinkedHashMap<>();
    if (payload.getParameters() != null) {
      templateParameters.putAll(payload.getParameters());
    }
    if (executionParameters != null) {
      templateParameters.putAll(executionParameters);
    }

    String uriTemplateSource = selectUriTemplateSource(payload.getUri(), taskCommand);
    String resolvedUri = resolveTemplateUrl(uriTemplateSource, templateParameters, coordinates);

    Set<String> uriTemplateParameters = extractUriTemplateParameters(uriTemplateSource);
    Map<String, String> queryParameters = new LinkedHashMap<>();
    if (payload.getParameters() != null) {
      queryParameters.putAll(payload.getParameters());
    }
    if (executionParameters != null) {
      executionParameters.forEach(queryParameters::putIfAbsent);
    }
    uriTemplateParameters.forEach(queryParameters::remove);
    return appendEncodedQueryParameters(resolvedUri, queryParameters);
  }

  private String appendEncodedQueryParameters(
      String resolvedUri, Map<String, String> queryParameters) {
    if (queryParameters.isEmpty()) {
      return resolvedUri;
    }
    StringBuilder url = new StringBuilder(resolvedUri);
    url.append(resolvedUri.contains("?") ? "&" : "?");
    List<String> encodedParameters = new ArrayList<>();
    queryParameters.forEach(
        (key, value) -> encodedParameters.add(encodeQueryComponent(key) + "=" + encodeQueryComponent(value)));
    url.append(String.join("&", encodedParameters));
    return url.toString();
  }

  private String encodeQueryComponent(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8)
        .replace("+", "%20");
  }

  private String selectUriTemplateSource(String payloadUri, String taskCommand) {
    if (extractUriTemplateParameters(payloadUri).isEmpty()
        && !extractUriTemplateParameters(taskCommand).isEmpty()) {
      return taskCommand;
    }
    return payloadUri;
  }

  private String readTaskCommand(Task task) {
    Map<String, Object> properties = task.getProperties();
    Object command = properties != null ? properties.get(DomainConstants.Tasks.PROPERTY_COMMAND) : null;
    return command != null ? String.valueOf(command) : null;
  }

  private Set<String> extractUriTemplateParameters(String uri) {
    if (!StringUtils.hasText(uri)) {
      return Collections.emptySet();
    }
    Matcher matcher = URI_TEMPLATE_PARAMETER_PATTERN.matcher(uri);
    Set<String> placeholders = new LinkedHashSet<>();
    while (matcher.find()) {
      placeholders.add(matcher.group(1));
    }
    return placeholders;
  }

  private String resolveTemplateUrl(
      String command, Map<String, String> parameters, RequestCoordinates coordinates) {
    String resolved = systemVariableResolver.resolve(command, coordinates);
    if (resolved == null) {
      resolved = command;
    }
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
      resolved = resolved.replace("{" + entry.getKey() + "}", encodedValue);
      resolved = resolved.replace("${" + entry.getKey() + "}", encodedValue);
    }
    return resolved;
  }

  private String readTemplateHtml(Task task) {
    Object raw = task.getProperties().get(DomainConstants.Tasks.PROPERTY_TEMPLATE_HTML);
    return raw != null ? String.valueOf(raw) : "";
  }

  private record MiaRenderContext(
      Map<String, Object> featureParameters,
      RequestCoordinates coordinates,
      Set<Integer> accessibleTaskIds) {}
}
