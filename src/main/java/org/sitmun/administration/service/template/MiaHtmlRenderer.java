package org.sitmun.administration.service.template;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MiaHtmlRenderer {

  private final Handlebars handlebars;

  public MiaHtmlRenderer() {
    this(createHandlebars());
  }

  MiaHtmlRenderer(Handlebars handlebars) {
    this.handlebars = handlebars;
  }

  public String tabs(String renderId, List<MiaPanelView> panels) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("renderId", nullToEmpty(renderId));
    context.put("panels", toPanelMaps(panels));
    return process("mia/tabs", context);
  }

  public String scroll(List<MiaPanelView> panels) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("panels", toPanelMaps(panels));
    return process("mia/scroll", context);
  }

  public String empty(String message) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("message", nullToEmpty(message));
    return process("mia/empty", context);
  }

  public String invalidChildTaskId(String localizedMessage) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("message", nullToEmpty(localizedMessage));
    return process("mia/error", context);
  }

  public String executionError(String localizedPrefix, String taskName) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("message", nullToEmpty(localizedPrefix) + ": " + nullToEmpty(taskName));
    return process("mia/error", context);
  }

  public String link(String url) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("url", nullToEmpty(url));
    return process("mia/link", context);
  }

  public String childError(String localizedPrefix, String taskName, String message) {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("prefix", nullToEmpty(localizedPrefix));
    context.put("taskName", nullToEmpty(taskName));
    context.put("message", nullToEmpty(message));
    return process("mia/childError", context);
  }

  public String table(List<Map<String, Object>> rows) {
    Set<String> columns = new LinkedHashSet<>();
    if (rows != null) {
      rows.forEach(row -> columns.addAll(row.keySet()));
    }
    List<String> columnList = new ArrayList<>(columns);
    List<List<String>> tableRows = new ArrayList<>();
    if (rows != null) {
      for (Map<String, Object> row : rows) {
        List<String> cells = new ArrayList<>(columnList.size());
        for (String column : columnList) {
          Object value = row.get(column);
          cells.add(value == null ? "" : String.valueOf(value));
        }
        tableRows.add(cells);
      }
    }
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("columns", columnList);
    context.put("rows", tableRows);
    return process("mia/table", context);
  }

  private String process(String templateName, Map<String, Object> context) {
    try {
      Template template = handlebars.compile(templateName);
      return template.apply(context).stripTrailing();
    } catch (IOException exception) {
      throw new UncheckedIOException(
          "Failed to render MIA chrome template " + templateName, exception);
    }
  }

  private static List<Map<String, Object>> toPanelMaps(List<MiaPanelView> panels) {
    List<Map<String, Object>> result = new ArrayList<>();
    if (panels == null) {
      return result;
    }
    for (MiaPanelView panel : panels) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("panelId", nullToEmpty(panel.panelId()));
      map.put("title", nullToEmpty(panel.title()));
      map.put("bodyHtml", panel.bodyHtml() == null ? "" : panel.bodyHtml());
      map.put(
          "cssClass", panel.active() ? "sitmun-mia-tab sitmun-mia-tab-active" : "sitmun-mia-tab");
      map.put("hiddenAttr", panel.active() ? "" : " style=\"display:none\"");
      result.add(map);
    }
    return result;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  static Handlebars createHandlebars() {
    TemplateLoader loader = new ClassPathTemplateLoader("/templates", ".hbs");
    return new Handlebars(loader);
  }
}
