package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.service.i18n.CurrentRequestLanguageResolver;
import org.sitmun.administration.service.i18n.TemplateLiteralProcessor;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.web.server.ResponseStatusException;

class TemplateRenderServiceTest {

  private TemplateRenderService createService(SystemVariableResolver resolver) {
    TemplateLiteralProcessor literalProcessor =
        new TemplateLiteralProcessor((literal, language) -> literal);
    return new TemplateRenderService(
        resolver,
        mock(TemplateRequestCoordinatesService.class),
        new TemplateContextNormalizer(),
        literalProcessor,
        mock(CurrentRequestLanguageResolver.class));
  }

  private TemplateRenderService createService(
      SystemVariableResolver resolver, TemplateLiteralProcessor literalProcessor) {
    return new TemplateRenderService(
        resolver,
        mock(TemplateRequestCoordinatesService.class),
        new TemplateContextNormalizer(),
        literalProcessor,
        mock(CurrentRequestLanguageResolver.class));
  }

  @Test
  void renderPreviewSupportsDottedFieldAndParameterLookups() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_NAME}"), any())).thenReturn("SITMUN");

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<h1>{{#APP_NAME}}</h1><p>{{pepe.nombre}}</p><span>{{pepe.$param1}}</span>",
            Map.of("pepe", Map.of("nombre", "Parcela 23-A", "$param1", "foo")),
            null);

    assertThat(response.getHtml()).contains("SITMUN").contains("Parcela 23-A").contains("foo");
    assertThat(response.getPlaceholders())
        .containsExactly("#APP_NAME", "pepe.nombre", "pepe.$param1");
  }

  @Test
  void renderPreviewReturnsControlledErrorForInvalidHandlebarsSyntax() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    assertThatThrownBy(() -> service.renderPreview("<p>tui name: {{task_</p>", Map.of(), null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Template preview contains invalid Handlebars syntax");
  }

  @Test
  void renderPreviewSupportsNestedJsonAccessWithArraySyntax() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<p>{{pepe.a[1].e}}</p><p>{{pepe.m}}</p>",
            Map.of("pepe", Map.of("a", List.of(Map.of("c", 1), Map.of("e", 2)), "m", 1)),
            null);

    assertThat(response.getHtml()).contains("<p>2</p>").contains("<p>1</p>");
    assertThat(response.getPlaceholders()).containsExactly("pepe.a[1].e", "pepe.m");
  }

  @Test
  void renderPreviewExpandsSitmunTableIterationMarkup() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<table data-sitmun-each=\"consulta_sql.rows\"><thead><tr><th>tui_name</th></tr></thead><tbody><tr><td>{{tui_name}}</td></tr></tbody></table>",
            Map.of(
                "consulta_sql",
                Map.of(
                    "rows",
                    List.of(
                        Map.of("tui_name", "sitna.layerCatalog"),
                        Map.of("tui_name", "sitna.search")))),
            null);

    assertThat(response.getHtml())
        .contains("<td>sitna.layerCatalog</td>")
        .contains("<td>sitna.search</td>")
        .doesNotContain("data-sitmun-each");
  }

  @Test
  void renderPreviewTranslatesTemplateLiteralsAfterRendering() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service =
        createService(
            resolver,
            new TemplateLiteralProcessor(
                (literal, language) ->
                    "es".equals(language) && "Hola món!".equals(literal)
                        ? "Hola mundo!"
                        : literal));

    TemplatePreviewResponseDto response =
        service.renderPreview("<p><t>Hola món!</t></p>", Map.of(), null, List.of(), "es");

    assertThat(response.getHtml()).isEqualTo("<p>Hola mundo!</p>");
  }

  @Test
  void renderPreviewNormalizesRootEachBlocksToRowsWhenContextProvidesRows() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "{{#each consulta_sql}}<p>{{this.tui_name}}</p>{{/each}}",
            Map.of(
                "consulta_sql",
                Map.of(
                    "rows",
                    List.of(
                        Map.of("tui_name", "sitna.layerCatalog"),
                        Map.of("tui_name", "sitna.search")))),
            null);

    assertThat(response.getHtml())
        .contains("<p>sitna.layerCatalog</p>")
        .contains("<p>sitna.search</p>");
  }

  @Test
  void renderPreviewExposesFirstNormalizedRowFieldsOnTaskAlias() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<p>{{FilteredHits.Player}} {{FilteredHits.AgeThatYear}} {{FilteredHits.Hits}} {{FilteredHits.id}}</p>",
            Map.of(
                "FilteredHits",
                Map.of(
                    "rows",
                    List.of(
                        Map.of("field", "items[0].Player", "value", "Ichiro Suzuki"),
                        Map.of("field", "items[0].AgeThatYear", "value", 30),
                        Map.of("field", "items[0].Hits", "value", 262),
                        Map.of("field", "items[0].id", "value", 1)))),
            null,
            List.of("FilteredHits"));

    assertThat(response.getHtml()).contains("<p>Ichiro Suzuki 30 262 1</p>");
  }

  @Test
  void renderPreviewNormalizesFlattenedRowsRecursivelyForEachBlocks() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "{{#each IcedCoffee}}<p>{{this.title}}</p>{{#each this.ingredients}}<span>{{this}}</span>{{/each}}{{/each}}",
            Map.of(
                "IcedCoffee",
                Map.of(
                    "rows",
                    List.of(
                        Map.of("field", "items[0].title", "value", "Iced Coffee"),
                        Map.of("field", "items[0].ingredients[0]", "value", "Coffee"),
                        Map.of("field", "items[0].ingredients[1]", "value", "Ice"),
                        Map.of("field", "items[1].title", "value", "Iced Espresso"),
                        Map.of("field", "items[1].ingredients[0]", "value", "Espresso"),
                        Map.of("field", "items[1].ingredients[1]", "value", "Ice")))),
            null);

    assertThat(response.getHtml())
        .contains("<p>Iced Coffee</p>")
        .contains("<span>Coffee</span>")
        .contains("<span>Ice</span>")
        .contains("<p>Iced Espresso</p>")
        .contains("<span>Espresso</span>");
  }

  @Test
  void renderPreviewKeepsUnresolvedTaskPlaceholdersVisibleWithExecutionHint() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

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

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview("<p>{{#APP_ID}}</p>", Map.of(), null);

    assertThat(response.getHtml()).contains("#APP_ID");
  }

  @Test
  void renderPreviewInsertsNestedTemplateHtmlWithoutEscapingMarkup() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<section>{{pepe.html}}</section>",
            Map.of("pepe", Map.of("html", "<p><strong>hola</strong></p>")),
            null);

    assertThat(response.getHtml()).contains("<section><p><strong>hola</strong></p></section>");
  }

  @Test
  void renderPreviewResolvesUserVariablesWhenResolverHasCurrentUserContext() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{USER_NAME}"), any())).thenReturn("admin");

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview("<p>{{#USER_NAME}}</p>", Map.of(), null);

    assertThat(response.getHtml()).contains("<p>admin</p>");
  }
}
