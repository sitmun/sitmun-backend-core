package org.sitmun.authorization.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.sitmun.authorization.client.AuthorizationConstants;
import org.sitmun.authorization.client.mapper.ProfileMapper;
import org.sitmun.authorization.client.service.TaskMapper;
import org.sitmun.domain.DomainConstants;
import org.sitmun.domain.task.parameter.TaskParameterProcessor;

/**
 * One task entry in the authorization client profile JSON ({@link ProfileDto#getTasks()}).
 *
 * <p><b>Construction</b>: {@link ProfileMapper} selects a {@link TaskMapper} bean; each mapper
 * populates only the subset of properties its task category needs. Absent JSON properties typically
 * mean {@link JsonInclude.Include#NON_NULL} / {@link JsonInclude.Include#NON_EMPTY} omission—not
 * that the conceptual task lacks data in persistence.
 *
 * <p><b>{@code parameters} vs {@code fields}</b>: for edition tasks ({@linkplain
 * AuthorizationConstants.TaskDto#EDITION}), {@code fields} carries the edition-mobile form schema
 * ({@code name}, {@code type}, {@code label}, …). {@code parameters} carries viewer/runtime slots
 * (including injected keys such as {@value AuthorizationConstants.TaskDto#PARAMETER_SERVICE}). For
 * other task kinds, {@code fields} is usually omitted. / {@value
 * DomainConstants.Tasks#PARAM_TYPE_BODY} — distinct from edition field data types ({@link
 * DomainConstants.Tasks#FIELD_TYPE_TEXT}, etc.).
 *
 * <p><b>{@code type} (DTO)</b>: query/cartography mappers set {@linkplain
 * AuthorizationConstants.TaskDto#SIMPLE}; edition sets {@linkplain
 * AuthorizationConstants.TaskDto#EDITION}. More-info tasks copy the persisted UI type string
 * ({@link org.sitmun.domain.task.Task#getUi}); basic tasks often leave this unset.
 *
 * <p><b>{@code scope} and related fields</b>: {@link #scope} is omitted for basic tasks,
 * cartography query profile entries, and edition tasks (those rely on {@link #url}, {@link
 * #parameters}, and for edition {@link #fields}; cartography tasks add {@link #layer} / {@link
 * #cartographyId}). When present, {@code scope} is the viewer-facing execution kind; admin stores
 * kebab-case values in {@value DomainConstants.Tasks#PROPERTY_SCOPE} ({@value
 * DomainConstants.Tasks#SCOPE_SQL_QUERY}, {@value DomainConstants.Tasks#SCOPE_WEB_API_QUERY}, …)
 * that {@link org.sitmun.domain.task.TaskScopeNormalizer} (and query mappers) map to the constants
 * below.
 *
 * <ul>
 *   <li><b>{@value DomainConstants.Tasks#SCOPE_SQL}</b> — {@link
 *       org.sitmun.authorization.client.service.TaskQuerySqlService}: {@link #url} SQL proxy
 *       endpoint; {@link #parameters} client SQL query parameters.
 *   <li><b>{@value DomainConstants.Tasks#SCOPE_API}</b> — proxied {@value
 *       DomainConstants.Tasks#SCOPE_WEB_API_QUERY} ({@link
 *       org.sitmun.authorization.client.service.TaskQueryWebService}): {@link #url} middleware HTTP
 *       proxy URL; optional {@link #mimeType}/{@link #filename} when the HTTP task exposes resource
 *       metadata.
 *   <li><b>{@value DomainConstants.Tasks#SCOPE_URL}</b> — {@linkplain
 *       org.sitmun.authorization.client.service.TaskQueryUrlService external-link} tasks, {@value
 *       DomainConstants.Tasks#SCOPE_WEB_API_QUERY_NO_PROXY} without MIME type, or more-info
 *       payloads normalized from {@value DomainConstants.Tasks#SCOPE_URL_QUERY}: {@link #url} is
 *       the direct command/external URL ({@linkplain
 *       org.sitmun.domain.DomainConstants.Tasks#PROPERTY_COMMAND command}-backed).
 *   <li><b>{@value DomainConstants.Tasks#SCOPE_RESOURCE}</b> — {@value
 *       DomainConstants.Tasks#SCOPE_WEB_API_QUERY_NO_PROXY} with {@linkplain
 *       org.sitmun.domain.DomainConstants.Tasks#PROPERTY_MIME_TYPE mimeType}, or the same split in
 *       more-info normalization: {@link #url} for fetch, with {@link #mimeType} and {@link
 *       #filename} guiding client handling.
 *   <li><b>Other / passthrough</b> — e.g. {@value DomainConstants.Tasks#SCOPE_CARTOGRAPHY_QUERY}
 *       may appear on more-info tasks when not mapped to {@value DomainConstants.Tasks#SCOPE_SQL};
 *       {@link #url} and {@link #parameters} depend on the related execution task ({@link
 *       org.sitmun.authorization.client.service.TaskMoreInfoService}).
 * </ul>
 */
@Getter
@Setter
@Builder
public class TaskDto {

  /** Profile task id ({@value DomainConstants.Tasks#TASK_PROFILE_ID_PREFIX}{@code <db id>}). */
  private String id;

  /** Optional UI wiring for basic/more-info tasks ({@code ui-control} in JSON). */
  @JsonProperty("ui-control")
  private String uiControl;

  /**
   * Execution URL: middleware proxy for {@value DomainConstants.Tasks#SCOPE_SQL} / {@value
   * DomainConstants.Tasks#SCOPE_API}, direct ({@linkplain
   * org.sitmun.domain.DomainConstants.Tasks#PROPERTY_COMMAND command}) URL for {@value
   * DomainConstants.Tasks#SCOPE_URL} / {@value DomainConstants.Tasks#SCOPE_RESOURCE},
   * cartography/edition proxies, or null. See class Javadoc scope section.
   */
  @JsonProperty("url")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String url;

  /**
   * Task category or UI type string (see class Javadoc). JSON property name {@code type}; not the
   * same as per-parameter {@code type} inside {@link #parameters}.
   */
  @JsonProperty("type")
  private String type;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Integer typeId;

  /**
   * Declared task parameters plus profile-injected slots (cartography/service/layers, SQL/API query
   * params, edition slots, …). Shape varies by {@link TaskMapper} and {@linkplain #scope} when set;
   * and {@link TaskParameterProcessor#toFeatureInfoParameter} for more-info.
   */
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Map<String, Object> parameters;

  /**
   * Edition form field definitions only; must not be used for generic task parameters (see class
   * Javadoc).
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private Map<String, Object> fields;

  /**
   * @deprecated Prefer {@linkplain #layer} ({@code layer/<id>}). Bare numeric legacy form.
   */
  @Deprecated()
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String cartographyId;

  /**
   * Profile {@code layers[].id} for this task's cartography ({@value
   * org.sitmun.domain.DomainConstants.Tasks#PROFILE_LAYER_ID_PREFIX}{@code id}).
   *
   * <p>Same convention as {@link org.sitmun.authorization.client.mapper.ProfileMapper} layer
   * payloads and tree node {@code resource}. Used for cartography query/edition entries where the
   * mapper does not set {@link #scope}.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String layer;

  /**
   * Viewer execution scope ({@value DomainConstants.Tasks#SCOPE_SQL}, {@value
   * DomainConstants.Tasks#SCOPE_API}, {@value DomainConstants.Tasks#SCOPE_URL}, {@value
   * DomainConstants.Tasks#SCOPE_RESOURCE}, or passthrough such as {@value
   * DomainConstants.Tasks#SCOPE_CARTOGRAPHY_QUERY}). Null when not materialized ({@linkplain
   * org.sitmun.authorization.client.service.TaskBasicService basic}, cartography-only query DTO,
   * {@linkplain AuthorizationConstants.TaskDto#EDITION edition}). See class Javadoc and {@link
   * org.sitmun.domain.task.TaskScopeNormalizer}.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String scope;

  /**
   * Reserved legacy profile slot; profile mappers currently leave this unset ({@code null}). URL
   * execution uses {@link #url}, typically sourced from persisted {@linkplain
   * org.sitmun.domain.DomainConstants.Tasks#PROPERTY_COMMAND command}.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String command;

  /** Human-readable task title for more-info tasks ({@code name} in JSON). */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("name")
  private String name;

  /**
   * MIME hint for {@value DomainConstants.Tasks#SCOPE_API} downloads and strongly tied to {@value
   * DomainConstants.Tasks#SCOPE_RESOURCE}; also drives RESOURCE vs URL split for {@value
   * DomainConstants.Tasks#SCOPE_WEB_API_QUERY_NO_PROXY} in {@link
   * org.sitmun.domain.task.TaskScopeNormalizer}.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String mimeType;

  /**
   * Filename hint paired with {@link #mimeType} for {@value DomainConstants.Tasks#SCOPE_RESOURCE} /
   * API payloads.
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String filename;
}
