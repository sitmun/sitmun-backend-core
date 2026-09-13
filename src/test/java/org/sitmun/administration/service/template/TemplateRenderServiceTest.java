package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.sitmun.administration.controller.dto.TemplatePreviewResponseDto;
import org.sitmun.administration.service.i18n.CurrentRequestLanguageResolver;
import org.sitmun.administration.service.i18n.TemplateLiteralProcessor;
import org.sitmun.authorization.proxy.service.RequestCoordinates;
import org.sitmun.infrastructure.variables.SystemVariableResolver;
import org.springframework.web.server.ResponseStatusException;

class TemplateRenderServiceTest {

  private static final Map<String, String> CONFIGURED_SYSTEM_VARS =
      Map.of(
          "USER_ID", "#{#user.id}",
          "USER_NAME", "#{#user.username}",
          "TERR_ID", "#{#territory.id}",
          "TERR_COD", "#{#territory.code}",
          "TERR_NAME", "#{#territory.name}",
          "APP_ID", "#{#application.id}",
          "APP_NAME", "#{#application.name}");

  private TemplateRenderService createService(SystemVariableResolver resolver) {
    TemplateLiteralProcessor literalProcessor =
        new TemplateLiteralProcessor((literal, language) -> literal);
    return createService(
        resolver,
        mock(TemplateRequestCoordinatesService.class),
        literalProcessor,
        mock(CurrentRequestLanguageResolver.class));
  }

  private TemplateRenderService createService(
      SystemVariableResolver resolver, TemplateLiteralProcessor literalProcessor) {
    return createService(
        resolver,
        mock(TemplateRequestCoordinatesService.class),
        literalProcessor,
        mock(CurrentRequestLanguageResolver.class));
  }

  private TemplateRenderService createService(
      SystemVariableResolver resolver,
      TemplateLiteralProcessor literalProcessor,
      CurrentRequestLanguageResolver languageResolver) {
    return createService(
        resolver,
        mock(TemplateRequestCoordinatesService.class),
        literalProcessor,
        languageResolver);
  }

  private TemplateRenderService createService(
      SystemVariableResolver resolver,
      TemplateRequestCoordinatesService coordinatesService,
      TemplateLiteralProcessor literalProcessor,
      CurrentRequestLanguageResolver languageResolver) {
    when(resolver.getAvailableVariables()).thenReturn(CONFIGURED_SYSTEM_VARS);
    when(coordinatesService.buildForCurrentUser()).thenReturn(new RequestCoordinates());
    return new TemplateRenderService(
        resolver,
        coordinatesService,
        new TemplateContextNormalizer(),
        literalProcessor,
        languageResolver,
        Clock.fixed(Instant.parse("2026-08-31T12:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void renderPreviewResolvesCurrentDateHelper() {
    TemplateRenderService service = createService(mock(SystemVariableResolver.class));

    TemplatePreviewResponseDto response = service.renderPreview("<p>{{currentDate}}</p>", Map.of());

    assertThat(response.getHtml()).isEqualTo("<p>31/08/2026</p>");
  }

  @Test
  void renderPreviewSupportsDottedFieldAndParameterLookups() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_NAME}"), any())).thenReturn("SITMUN");

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<h1>{{#APP_NAME}}</h1><p>{{pepe.nombre}}</p><span>{{pepe.$param1}}</span>",
            Map.of("pepe", Map.of("nombre", "Parcela 23-A", "$param1", "foo")));

    assertThat(response.getHtml()).contains("SITMUN").contains("Parcela 23-A").contains("foo");
    assertThat(response.getPlaceholders())
        .containsExactly("#APP_NAME", "pepe.nombre", "pepe.$param1");
  }

  @Test
  void renderPreviewReturnsControlledErrorForInvalidHandlebarsSyntax() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    assertThatThrownBy(() -> service.renderPreview("<p>tui name: {{task_</p>", Map.of()))
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
            Map.of("pepe", Map.of("a", List.of(Map.of("c", 1), Map.of("e", 2)), "m", 1)));

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
                        Map.of("tui_name", "sitna.search")))));

    assertThat(response.getHtml())
        .contains("<td>sitna.layerCatalog</td>")
        .contains("<td>sitna.search</td>")
        .doesNotContain("data-sitmun-each");
  }

  @Test
  void renderPreviewKeepsTipTapHeaderRowOutsideSitmunEach() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    // TipTap serializes header cells as <th> rows inside <tbody> (no <thead>).
    String tipTapTable =
        "<table data-sitmun-each=\"consulta_sql.rows\">"
            + "<tbody>"
            + "<tr><th><p>tui_name</p></th></tr>"
            + "<tr><td><p>{{tui_name}}</p></td></tr>"
            + "</tbody></table>";

    TemplatePreviewResponseDto response =
        service.renderPreview(
            tipTapTable,
            Map.of(
                "consulta_sql",
                Map.of(
                    "rows",
                    List.of(
                        Map.of("tui_name", "sitna.layerCatalog"),
                        Map.of("tui_name", "sitna.search")))));

    String html = response.getHtml();
    assertThat(html)
        .contains("sitna.layerCatalog")
        .contains("sitna.search")
        .doesNotContain("data-sitmun-each");
    assertThat(html.split("tui_name", -1)).hasSize(2);
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
        service.renderPreview("<p><t>Hola món!</t></p>", Map.of(), List.of(), "es");

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
                        Map.of("tui_name", "sitna.search")))));

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
                        Map.of("field", "items[1].ingredients[1]", "value", "Ice")))));

    assertThat(response.getHtml())
        .contains("<p>Iced Coffee</p>")
        .contains("<span>Coffee</span>")
        .contains("<span>Ice</span>")
        .contains("<p>Iced Espresso</p>")
        .contains("<span>Espresso</span>");
  }

  @Test
  void renderPreviewMarksUnresolvedTaskPlaceholdersAsColoredOriginalMustache() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<p>{{pepe.name}}</p><p>{{consulta.url}}</p>",
            Map.of("pepe", Map.of("name", "Parcela 23-A")),
            List.of("pepe", "consulta"));

    assertThat(response.getHtml())
        .contains("<p>Parcela 23-A</p>")
        .contains("class=\"sitmun-template-error\"")
        .contains("&#123;&#123;consulta.url&#125;&#125;")
        .doesNotContain("task not executed")
        .doesNotContain("sitmun-template-placeholder");
  }

  @Test
  void renderPreviewUsesBareNameForKnownUnresolvedSystemVariable() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_ID}"), any())).thenReturn("#{APP_ID}");

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response = service.renderPreview("<p>{{#APP_ID}}</p>", Map.of());

    assertThat(response.getHtml())
        .contains("class=\"sitmun-template-known\"")
        .contains(">APP_ID<")
        .doesNotContain("{{#APP_ID}}")
        .doesNotContain("sitmun-template-error")
        .doesNotContain("sitmun-template-placeholder");
  }

  @Test
  void renderPreviewKeepsKnownUnresolvedSystemVariableAttributeSafe() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_NAME}"), any())).thenReturn("#{APP_NAME}");

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview(
            "<img src=\"https://example.test/x.png\" title=\"{{#APP_NAME}}\" alt=\"{{#APP_NAME}}\">",
            Map.of());

    assertThat(response.getHtml())
        .contains("title=\"APP_NAME\"")
        .contains("alt=\"APP_NAME\"")
        .doesNotContain("title=\"<span")
        .doesNotContain("sitmun-template-known")
        .doesNotContain("{{#APP_NAME}}");
  }

  @Test
  void renderPreviewKeepsUnresolvedTaskPlaceholderAttributeSafe() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response =
        service.renderPreview("<img src=\"{{foto.url}}\">", Map.of(), List.of("foto"));

    assertThat(response.getHtml())
        .contains("src=\"&#123;&#123;foto.url&#125;&#125;\"")
        .doesNotContain("src=\"<span")
        .doesNotContain("sitmun-template-error");
  }

  @Test
  void renderPreviewMarksUnknownSystemVariableAsColoredOriginalMustache() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{NOT_A_VAR}"), any())).thenReturn("#{NOT_A_VAR}");

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response = service.renderPreview("<p>{{#NOT_A_VAR}}</p>", Map.of());

    assertThat(response.getHtml())
        .contains("class=\"sitmun-template-error\"")
        .contains("&#123;&#123;#NOT_A_VAR&#125;&#125;")
        .doesNotContain("<p>NOT_A_VAR</p>");
  }

  @Test
  void renderPreviewUsesAppAndTerritoryCoordinatesWhenProvided() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_NAME}"), any())).thenReturn("Sitmun");
    TemplateRequestCoordinatesService coordinatesService =
        mock(TemplateRequestCoordinatesService.class);
    RequestCoordinates withApp = new RequestCoordinates();
    when(coordinatesService.buildOptional(12, 4)).thenReturn(withApp);
    when(coordinatesService.buildForCurrentUser()).thenReturn(new RequestCoordinates());

    TemplateRenderService service =
        createService(
            resolver,
            coordinatesService,
            new TemplateLiteralProcessor((literal, language) -> literal),
            mock(CurrentRequestLanguageResolver.class));

    TemplatePreviewResponseDto response =
        service.renderPreview("<p>{{#APP_NAME}}</p>", Map.of(), List.of(), null, 12, 4);

    assertThat(response.getHtml()).contains("<p>Sitmun</p>");
    verify(coordinatesService).buildOptional(12, 4);
  }

  @Test
  void renderPreviewInsertsNestedTemplateHtmlWithoutEscapingMarkup() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    TemplateRenderService service = createService(resolver);

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

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response = service.renderPreview("<p>{{#USER_NAME}}</p>", Map.of());

    assertThat(response.getHtml()).contains("<p>admin</p>");
  }

  @Test
  void renderPreviewTreatsResolvedSystemVariableWithHandlebarsDelimitersAsOpaque() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_NAME}"), any())).thenReturn("{{evil}}");

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response = service.renderPreview("<p>{{#APP_NAME}}</p>", Map.of());

    assertThat(response.getHtml())
        .isEqualTo("<p>&#123;&#123;evil&#125;&#125;</p>")
        .doesNotContain("{{evil}}");
  }

  @Test
  void renderPreviewEscapesHtmlInResolvedSystemVariableValues() {
    SystemVariableResolver resolver = mock(SystemVariableResolver.class);
    when(resolver.resolve(eq("#{APP_NAME}"), any())).thenReturn("<b>x</b>");

    TemplateRenderService service = createService(resolver);

    TemplatePreviewResponseDto response = service.renderPreview("<p>{{#APP_NAME}}</p>", Map.of());

    assertThat(response.getHtml())
        .isEqualTo("<p>&lt;b&gt;x&lt;/b&gt;</p>")
        .doesNotContain("<b>x</b>");
  }
}
