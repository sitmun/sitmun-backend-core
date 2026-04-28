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
            "<h1>{{#APP_NAME}}</h1><p>{{pepe.nombre}}</p><span>{{pepe.$param1}}</span>",
            Map.of("pepe", Map.of("nombre", "Parcela 23-A", "$param1", "foo")));

    assertThat(response.getHtml()).contains("SITMUN").contains("Parcela 23-A").contains("foo");
    assertThat(response.getPlaceholders()).containsExactly("#APP_NAME", "pepe.nombre", "pepe.$param1");
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
            "<p>{{pepe.a[1].e}}</p><p>{{pepe.m}}</p>",
            Map.of(
                "pepe",
                Map.of(
                    "a", List.of(Map.of("c", 1), Map.of("e", 2)),
                    "m", 1)));

    assertThat(response.getHtml()).contains("<p>2</p>").contains("<p>1</p>");
    assertThat(response.getPlaceholders()).containsExactly("pepe.a[1].e", "pepe.m");
  }

  @Test
  void renderPreviewExpandsSitmunTableIterationAttributes() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<table data-sitmun-each=\"consulta_sql.rows\"><thead><tr><th>tui_name</th></tr></thead><tbody><tr><td>{{tui_name}}</td></tr></tbody></table>",
            Map.of(
                "consulta_sql",
                Map.of(
                    "rows",
                    List.of(
                        Map.of("tui_name", "sitna.layerCatalog"),
                        Map.of("tui_name", "sitna.search")))));

    assertThat(response.getHtml())
        .contains("<td>sitna.layerCatalog</td>")
        .contains("<td>sitna.search</td>")
        .doesNotContain("data-sitmun-each");
  }

  @Test
  void renderPreviewExpandsQuillTableBetterMarkupAfterHeaderEditing() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    String templateHtml = "<table class=\"ql-table-better\"><temporary class=\"ql-table-temporary\" data-class=\"ql-table-better\"></temporary>"
        + "<thead><tr><th data-row=\"1\"><p class=\"table-th-block\" data-cell=\"1\" data-sitmun-each=\"task_32281.rows\">tui_tooltip a</p></th></tr></thead>"
        + "<tbody><tr><td data-row=\"2\"><p class=\"ql-table-block\" data-cell=\"1\" data-sitmun-each=\"task_32281.rows\">{{tui_tooltip}}</p></td></tr></tbody></table>";

    TemplatePreviewResponseDto response =
        service.renderPreview(
            templateHtml,
            Map.of(
                "task_32281",
                Map.of(
                    "rows",
                    List.of(
                        Map.of("tui_tooltip", "layerCatalog"),
                        Map.of("tui_tooltip", "search")))));

    assertThat(response.getHtml())
        .contains("tui_tooltip a")
        .contains("layerCatalog")
        .contains("search")
        .contains("ql-table-better")
        .doesNotContain("<temporary")
        .doesNotContain("data-sitmun-each");
  }

  @Test
  void renderPreviewKeepsUnresolvedTaskPlaceholdersVisibleWithExecutionHint() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = new TemplateRenderService(resolver, mock(TemplateRequestCoordinatesService.class));

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<p>{{pepe.name}}</p><p>{{consulta.url}}</p>",
            Map.of("pepe", Map.of("name", "Parcela 23-A")),
            null,
            List.of("pepe", "consulta"));

    assertThat(response.getHtml())
        .contains("<p>Parcela 23-A</p>")
        .contains("consulta.url")
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
            "<section>{{pepe.html}}</section>",
            Map.of("pepe", Map.of("html", "<p><strong>hola</strong></p>")));

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
