package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MiaHtmlRendererTest {

  private MiaHtmlRenderer renderer;

  @BeforeEach
  void setUp() {
    renderer = new MiaHtmlRenderer();
  }

  @Test
  void tabsMatchesLegacyStringBuilderContract() {
    List<MiaPanelView> panels =
        List.of(
            new MiaPanelView("mia-backend-1-0", "A&B", "<b>1</b>", true),
            new MiaPanelView("mia-backend-1-1", "O'Brien", "two", false));

    assertThat(renderer.tabs("mia-backend-1", panels))
        .isEqualTo(
            """
            <div class="sitmun-mia-tabs-bar" data-mia-tabs="mia-backend-1">\
            <button class="sitmun-mia-tab sitmun-mia-tab-active" data-mia-tab="mia-backend-1-0">A&amp;B</button>\
            <button class="sitmun-mia-tab" data-mia-tab="mia-backend-1-1">O&#x27;Brien</button>\
            </div><div class="sitmun-mia-body">\
            <div class="sitmun-mia-tab-panel" data-mia-panel="mia-backend-1-0"><b>1</b></div>\
            <div class="sitmun-mia-tab-panel" data-mia-panel="mia-backend-1-1" style="display:none">two</div>\
            </div>\
            """);
  }

  @Test
  void scrollMatchesLegacyStringBuilderContract() {
    List<MiaPanelView> panels =
        List.of(
            new MiaPanelView("p0", "A&B", "<b>1</b>", true),
            new MiaPanelView("p1", "O'Brien", "two", false));

    assertThat(renderer.scroll(panels))
        .isEqualTo(
            """
            <div class="sitmun-mia-body sitmun-mia-scroll-body">\
            <div class="sitmun-mia-scroll-section"><div class="sitmun-mia-section-title">A&amp;B</div><b>1</b></div>\
            <div class="sitmun-mia-scroll-section"><div class="sitmun-mia-section-title">O&#x27;Brien</div>two</div>\
            </div>\
            """);
  }

  @Test
  void emptyEscapesMessage() {
    assertThat(renderer.empty("No data"))
        .isEqualTo("<div class=\"sitmun-mia-empty\">No data</div>");
    assertThat(renderer.empty("a<b>\"c'd"))
        .isEqualTo("<div class=\"sitmun-mia-empty\">a&lt;b&gt;&quot;c&#x27;d</div>");
  }

  @Test
  void invalidChildTaskIdEscapesLocalizedMessage() {
    assertThat(renderer.invalidChildTaskId("Invalid child task id"))
        .isEqualTo("<div class=\"sitmun-mia-error\">Invalid child task id</div>");
  }

  @Test
  void executionErrorEscapesPrefixAndTaskName() {
    assertThat(renderer.executionError("Error executing task", "T&1"))
        .isEqualTo("<div class=\"sitmun-mia-error\">Error executing task: T&amp;1</div>");
    assertThat(renderer.executionError("A&B <x>", "T'1"))
        .isEqualTo("<div class=\"sitmun-mia-error\">A&amp;B &lt;x&gt;: T&#x27;1</div>");
  }

  @Test
  void linkEscapesUrlInHrefAndText() {
    assertThat(renderer.link("https://x.test?a=1&b=2"))
        .isEqualTo(
            "<a href=\"https://x.test?a&#x3D;1&amp;b&#x3D;2\" target=\"_blank\" rel=\"noopener noreferrer\">"
                + "https://x.test?a&#x3D;1&amp;b&#x3D;2</a>");
  }

  @Test
  void childErrorMatchesLegacyContract() {
    assertThat(renderer.childError("Error executing task", "T'1", "bad&"))
        .isEqualTo(
            "<div class=\"sitmun-template-child-error\">Error executing task: T&#x27;1 - bad&amp;</div>");
  }

  @Test
  void tableUsesFirstSeenColumnOrderAndEscapesCells() {
    Map<String, Object> row1 = new LinkedHashMap<>();
    row1.put("b", 2);
    row1.put("a", "x&y");
    Map<String, Object> row2 = new LinkedHashMap<>();
    row2.put("a", 3);
    row2.put("c", 4);

    assertThat(renderer.table(List.of(row1, row2)))
        .isEqualTo(
            """
            <table class="sitmun-json-table"><thead><tr><th>b</th><th>a</th><th>c</th></tr></thead>\
            <tbody><tr><td>2</td><td>x&amp;y</td><td></td></tr>\
            <tr><td></td><td>3</td><td>4</td></tr></tbody></table>\
            """);
  }
}
