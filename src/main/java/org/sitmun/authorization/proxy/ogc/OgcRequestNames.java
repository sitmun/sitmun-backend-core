package org.sitmun.authorization.proxy.ogc;

import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Layer names and the OGC operation taken from a proxy request. Parameter names are matched without
 * regard to case. A body that is not well-formed XML, or whose root is not a known WFS element, is
 * rejected so a transaction cannot hide behind another operation name.
 */
public final class OgcRequestNames {

  public enum Operation {
    GET_MAP,
    GET_FEATURE_INFO,
    DESCRIBE_LAYER,
    GET_LEGEND_GRAPHIC,
    GET_CAPABILITIES,
    GET_FEATURE,
    DESCRIBE_FEATURE_TYPE,
    TRANSACTION,
    OTHER
  }

  public record View(
      Operation operation,
      Set<String> layers,
      Set<String> queryLayers,
      Set<String> legendLayers,
      Set<String> typeNames,
      boolean rejected) {}

  private OgcRequestNames() {}

  public static View parse(Map<String, String> parameters, String body) {
    Set<String> layers = values(parameters, "LAYERS");
    Set<String> queryLayers = values(parameters, "QUERY_LAYERS");
    Set<String> legendLayers = values(parameters, "LAYER");
    Set<String> typeNames = new LinkedHashSet<>();
    typeNames.addAll(values(parameters, "TYPENAME"));
    typeNames.addAll(values(parameters, "TYPENAMES"));

    Operation fromQuery = operation(first(parameters, "REQUEST"));
    boolean rejected = false;
    Operation fromBody = null;
    if (body != null && !body.isBlank()) {
      try {
        XmlBody parsed = readBody(body);
        fromBody = parsed.operation;
        typeNames.addAll(parsed.typeNames);
        if (parsed.operation == Operation.OTHER) {
          rejected = true;
        }
      } catch (XMLStreamException ex) {
        rejected = true;
      }
    }
    if (fromQuery != Operation.OTHER && fromBody != null && fromQuery != fromBody) {
      rejected = true;
    }
    Operation operation = fromBody != null ? fromBody : fromQuery;
    return new View(operation, layers, queryLayers, legendLayers, typeNames, rejected);
  }

  private static Operation operation(String request) {
    if (request == null || request.isBlank()) {
      return Operation.OTHER;
    }
    return switch (request.trim().toLowerCase(Locale.ROOT)) {
      case "getmap" -> Operation.GET_MAP;
      case "getfeatureinfo" -> Operation.GET_FEATURE_INFO;
      case "describelayer" -> Operation.DESCRIBE_LAYER;
      case "getlegendgraphic" -> Operation.GET_LEGEND_GRAPHIC;
      case "getcapabilities" -> Operation.GET_CAPABILITIES;
      case "getfeature" -> Operation.GET_FEATURE;
      case "describefeaturetype" -> Operation.DESCRIBE_FEATURE_TYPE;
      case "transaction" -> Operation.TRANSACTION;
      default -> Operation.OTHER;
    };
  }

  private static Set<String> values(Map<String, String> parameters, String name) {
    Set<String> values = new LinkedHashSet<>();
    if (parameters == null) {
      return values;
    }
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      if (entry.getKey() != null && name.equalsIgnoreCase(entry.getKey())) {
        values.addAll(split(entry.getValue(), ","));
      }
    }
    return values;
  }

  private static String first(Map<String, String> parameters, String name) {
    if (parameters == null) {
      return null;
    }
    for (Map.Entry<String, String> entry : parameters.entrySet()) {
      if (entry.getKey() != null && name.equalsIgnoreCase(entry.getKey())) {
        return entry.getValue();
      }
    }
    return null;
  }

  private static Set<String> split(String raw, String pattern) {
    Set<String> values = new LinkedHashSet<>();
    if (raw == null || raw.isBlank()) {
      return values;
    }
    Arrays.stream(raw.split(pattern))
        .map(String::trim)
        .filter(part -> !part.isEmpty())
        .forEach(values::add);
    return values;
  }

  private static XmlBody readBody(String body) throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(body));
    try {
      String root = null;
      Set<String> typeNames = new LinkedHashSet<>();
      boolean inInsert = false;
      while (reader.hasNext()) {
        int event = reader.next();
        if (event != XMLStreamConstants.START_ELEMENT) {
          if (event == XMLStreamConstants.END_ELEMENT
              && "Insert".equalsIgnoreCase(reader.getLocalName())) {
            inInsert = false;
          }
          continue;
        }
        String local = reader.getLocalName();
        if (root == null) {
          root = local;
          collectTypeAttributes(reader, typeNames);
          continue;
        }
        if ("Insert".equalsIgnoreCase(local)) {
          inInsert = true;
          continue;
        }
        if (inInsert) {
          typeNames.add(qualified(reader));
          inInsert = false;
          continue;
        }
        collectTypeAttributes(reader, typeNames);
      }
      return new XmlBody(operation(root), typeNames);
    } finally {
      reader.close();
    }
  }

  private static void collectTypeAttributes(XMLStreamReader reader, Set<String> typeNames) {
    for (int i = 0; i < reader.getAttributeCount(); i++) {
      String name = reader.getAttributeLocalName(i);
      if ("typeName".equalsIgnoreCase(name) || "typeNames".equalsIgnoreCase(name)) {
        typeNames.addAll(split(reader.getAttributeValue(i), "[,\\s]+"));
      }
    }
  }

  private static String qualified(XMLStreamReader reader) {
    String prefix = reader.getPrefix();
    if (prefix == null || prefix.isBlank()) {
      return reader.getLocalName();
    }
    return prefix + ":" + reader.getLocalName();
  }

  private record XmlBody(Operation operation, Set<String> typeNames) {}
}
