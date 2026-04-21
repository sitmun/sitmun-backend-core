package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.web.server.ResponseStatusException;

class TemplateRenderServiceTest {

  @Test
  void renderPreviewSupportsDottedFieldAndParameterLookups() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_NAME}"), any())).thenReturn("SITMUN");

    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<h1>{{#APP_NAME}}</h1><p>{{task_13.nombre}}</p><span>{{task_13.$param1}}</span>",
            Map.of("task_13", Map.of("nombre", "Parcela 23-A", "$param1", "foo")));

    assertThat(response.getHtml()).contains("SITMUN").contains("Parcela 23-A").contains("foo");
    assertThat(response.getPlaceholders())
        .containsExactly("#APP_NAME", "task_13.nombre", "task_13.$param1");
  }

  @Test
  void renderPreviewReturnsControlledErrorForInvalidHandlebarsSyntax() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    assertThatThrownBy(() -> service.renderPreview("<p>tui name: {{task_</p>", Map.of()))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Template preview contains invalid Handlebars syntax");
  }

  @Test
  void renderPreviewSupportsNestedJsonAccessWithArraySyntax() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);

    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<p>{{task_13.a[1].e}}</p><p>{{task_13.m}}</p>",
            Map.of(
                "task_13",
                Map.of(
                    "a", List.of(Map.of("c", 1), Map.of("e", 2)),
                    "m", 1)));

    assertThat(response.getHtml()).contains("<p>2</p>").contains("<p>1</p>");
    assertThat(response.getPlaceholders()).containsExactly("task_13.a[1].e", "task_13.m");
  }

  @Test
  void renderPreviewKeepsUnresolvedTaskPlaceholdersVisibleWithExecutionHint() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<p>{{task_13.name}}</p><p>{{task_99.url}}</p>",
            Map.of("task_13", Map.of("name", "Parcela 23-A")));

    assertThat(response.getHtml())
        .contains("<p>Parcela 23-A</p>")
        .contains("task_99.url")
        .contains("(falta ejecutar tarea)");
  }

  @Test
  void renderPreviewKeepsSystemVariablePlaceholderWhenValueCannotBeResolved() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_ID}"), any())).thenReturn("#{APP_ID}");

    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    TemplatePreviewResponseDto response = service.renderPreview("<p>{{#APP_ID}}</p>", Map.of());

    assertThat(response.getHtml()).contains("#APP_ID");
  }

  @Test
  void renderPreviewInsertsNestedTemplateHtmlWithoutEscapingMarkup() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));
  
    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<section>{{task_96.html}}</section>",
            Map.of("task_96", Map.of("html", "<p><strong>hola</strong></p>")));

    assertThat(response.getHtml()).contains("<section><p><strong>hola</strong></p></section>");
  }

  @Test
  void renderPreviewResolvesUserVariablesWhenResolverHasCurrentUserContext() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{USER_NAME}"), any())).thenReturn("admin");

    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    TemplatePreviewResponseDto response = service.renderPreview("<p>{{#USER_NAME}}</p>", Map.of());

    assertThat(response.getHtml()).contains("<p>admin</p>");
  }
}
