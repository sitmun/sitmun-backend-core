package org.sitmun.administration.service.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import okio.Timeout;
import org.sitmun.administration.service.database.DatabaseConnectionService;
import org.sitmun.administration.service.extractor.HttpClientFactory;
import org.sitmun.administration.service.i18n.CurrentRequestLanguageResolver;
import org.sitmun.administration.service.i18n.LiteralTranslationResolver;
import org.sitmun.administration.service.template.childdata.TemplateChildDataService;
import org.sitmun.authorization.access.UserApplicationAccessPolicy;
import org.sitmun.authorization.proxy.service.ProxyConfigurationService;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.domain.role.Role;
import org.sitmun.domain.role.RoleRepository;
import org.sitmun.domain.task.Task;
import org.sitmun.domain.task.TaskRepository;
import org.sitmun.domain.task.relation.TaskRelation;
import org.sitmun.domain.task.relation.TaskRelationRepository;
import org.sitmun.domain.territory.Territory;
import org.sitmun.domain.user.User;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class TemplateExecutionServiceTestFixtures {
  protected void authenticateAdmin() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
  }

  protected TemplateExecutionService newService(
      TaskRepository taskRepository,
      TaskRelationRepository taskRelationRepository,
      ProxyConfigurationService proxyConfigurationService,
      DatabaseConnectionService databaseConnectionService,
      HttpClientFactory httpClientFactory,
      SystemVariableResolver systemVariableResolver,
      TemplateRenderService templateRenderService,
      TemplateRequestCoordinatesService coordinatesService,
      ObjectMapper objectMapper) {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      authenticateAdmin();
    }
    RoleRepository roleRepository = mock(RoleRepository.class);
    when(roleRepository.findRolesByApplicationAndUserAndTerritory(any(), any(), any()))
        .thenReturn(List.of(Role.builder().id(3).build()));
    // Permissive default for render tests that previously relied on ADMIN short-circuit.
    // Includes findById stubs and relatedTask edges from TaskRelationRepository stubbings.
    // Denial tests override with thenReturn(List.of()) / parent-only lists after newService.
    lenient()
        .when(taskRepository.findByRolesAndTerritory(any(), any()))
        .thenAnswer(
            invocation -> {
              java.util.LinkedHashSet<Task> available = new java.util.LinkedHashSet<>();
              for (org.mockito.stubbing.Stubbing stubbing :
                  org.mockito.Mockito.mockingDetails(taskRepository).getStubbings()) {
                if (!"findById".equals(stubbing.getInvocation().getMethod().getName())) {
                  continue;
                }
                Object id = stubbing.getInvocation().getArgument(0);
                if (id instanceof Integer taskId) {
                  taskRepository.findById(taskId).ifPresent(available::add);
                }
              }
              for (org.mockito.stubbing.Stubbing stubbing :
                  org.mockito.Mockito.mockingDetails(taskRelationRepository).getStubbings()) {
                if (!"findByTaskId".equals(stubbing.getInvocation().getMethod().getName())) {
                  continue;
                }
                Object id = stubbing.getInvocation().getArgument(0);
                if (!(id instanceof Integer taskId)) {
                  continue;
                }
                for (TaskRelation relation : taskRelationRepository.findByTaskId(taskId)) {
                  if (relation.getTask() != null) {
                    available.add(relation.getTask());
                  }
                  if (relation.getRelatedTask() != null) {
                    available.add(relation.getRelatedTask());
                  }
                }
              }
              return new java.util.ArrayList<>(available);
            });
    UserApplicationAccessPolicy accessPolicy = mock(UserApplicationAccessPolicy.class);
    when(proxyConfigurationService.validateUserAccess(any(), any())).thenReturn(true);
    LiteralTranslationResolver literalTranslationResolver = chromeLiteralResolver();
    CurrentRequestLanguageResolver currentRequestLanguageResolver =
        mock(CurrentRequestLanguageResolver.class);
    lenient()
        .when(currentRequestLanguageResolver.resolve(any()))
        .thenAnswer(
            invocation -> {
              if (RequestContextHolder.getRequestAttributes()
                  instanceof ServletRequestAttributes attributes) {
                String language = attributes.getRequest().getParameter("lang");
                if (language != null && !language.isBlank()) {
                  return language;
                }
              }
              return null;
            });
    TemplateChildDataService childDataService =
        new TemplateChildDataService(
            proxyConfigurationService,
            databaseConnectionService,
            httpClientFactory,
            systemVariableResolver,
            literalTranslationResolver,
            currentRequestLanguageResolver,
            objectMapper);
    return new TemplateExecutionService(
        taskRepository,
        roleRepository,
        taskRelationRepository,
        templateRenderService,
        coordinatesService,
        accessPolicy,
        childDataService,
        literalTranslationResolver,
        currentRequestLanguageResolver,
        new MiaHtmlRenderer(),
        mock(org.sitmun.administration.service.mapimage.MapImageTaskExecutionService.class),
        objectMapper);
  }

  protected static LiteralTranslationResolver chromeLiteralResolver() {
    LiteralTranslationResolver literalTranslationResolver = mock(LiteralTranslationResolver.class);
    Map<String, Map<String, String>> translations =
        Map.of(
            "No data",
            Map.of("ca", "Sense dades", "es", "Sin datos", "en", "No data", "fr", "Aucune donnee"),
            "Error executing task",
            Map.of(
                "ca",
                "Error executant la tasca",
                "es",
                "Error ejecutando tarea",
                "en",
                "Error executing task",
                "fr",
                "Erreur d execution de la tache"),
            "Query",
            Map.of("ca", "Consulta", "es", "Consulta", "en", "Query", "fr", "Requete"),
            "Invalid child task id",
            Map.of(
                "ca",
                "Identificador de tasca fill no valid",
                "es",
                "Id de tarea hija no valido",
                "en",
                "Invalid child task id",
                "fr",
                "Identifiant de tache enfant invalide"),
            "task not executed",
            Map.of(
                "ca",
                "cal executar la tasca",
                "es",
                "falta ejecutar tarea",
                "en",
                "task not executed",
                "fr",
                "tache non executee"),
            "Binary content cannot be embedded: server authentication required",
            Map.of(
                "ca",
                "Contingut binari no incrustable: cal autenticacio de servidor",
                "es",
                "Contenido binario no embebible: requiere autenticacion de servidor",
                "en",
                "Binary content cannot be embedded: server authentication required",
                "fr",
                "Contenu binaire non integrable: authentification serveur requise"),
            "[binary content]",
            Map.of(
                "ca",
                "[contingut binari]",
                "es",
                "[contenido binario]",
                "en",
                "[binary content]",
                "fr",
                "[contenu binaire]"));
    lenient()
        .when(literalTranslationResolver.resolve(any(), any()))
        .thenAnswer(
            invocation -> {
              String key = invocation.getArgument(0);
              String language = invocation.getArgument(1);
              if (key == null) {
                return null;
              }
              if (language == null || language.isBlank()) {
                return key;
              }
              String normalized = language.trim().toLowerCase();
              String lang =
                  normalized.startsWith("es")
                      ? "es"
                      : normalized.startsWith("en")
                          ? "en"
                          : normalized.startsWith("fr") ? "fr" : "ca";
              Map<String, String> byLang = translations.get(key);
              if (byLang == null) {
                return key;
              }
              return byLang.getOrDefault(lang, key);
            });
    return literalTranslationResolver;
  }

  protected RequestCoordinates requestCoordinatesWithUserPermission(Integer territoryId) {
    Territory territory = Territory.builder().id(territoryId).build();
    User user = User.builder().username("viewer").permissions(new HashSet<>()).build();
    RequestCoordinates coordinates = new RequestCoordinates();
    coordinates.setApplication(org.sitmun.domain.application.Application.builder().id(5).build());
    coordinates.setUser(user);
    coordinates.setTerritory(territory);
    return coordinates;
  }

  protected static final class ThrowingStringResponseBody extends ResponseBody {
    private final okhttp3.MediaType contentType;

    protected ThrowingStringResponseBody(String contentType) {
      this.contentType = okhttp3.MediaType.parse(contentType);
    }

    @Override
    public okhttp3.MediaType contentType() {
      return contentType;
    }

    @Override
    public long contentLength() {
      return 4;
    }

    @Override
    public BufferedSource source() {
      return Okio.buffer(
          new Source() {
            @Override
            public long read(okio.Buffer sink, long byteCount) {
              throw new AssertionError("Binary response body must not be read as text");
            }

            @Override
            public Timeout timeout() {
              return Timeout.NONE;
            }

            @Override
            public void close() {}
          });
    }
  }
}
