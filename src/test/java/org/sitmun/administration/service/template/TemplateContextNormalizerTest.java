package org.sitmun.administration.service.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateContextNormalizerTest {

  private final TemplateContextNormalizer normalizer = new TemplateContextNormalizer();

  @Test
  void returnsEmptyMapForNullContext() {
    assertThat(normalizer.normalize(null)).isEmpty();
  }

  @Test
  void keepsAlreadyNormalizedRows() {
    Map<String, Object> context = Map.of(
        "FilteredHits",
        Map.of("rows", List.of(Map.of("Player", "Ichiro", "Hits", 262))));

    Map<String, Object> normalized = normalizer.normalize(context);

    assertThat(normalized)
        .containsKey("FilteredHits");
    assertThat((Map<String, Object>) normalized.get("FilteredHits"))
        .containsEntry("Player", "Ichiro")
        .containsEntry("Hits", 262);
  }

  @Test
  void normalizesFlattenedItemsRowsToObjectRows() {
    Map<String, Object> normalized = normalizer.normalize(Map.of(
        "FilteredHits",
        Map.of(
            "rows",
            List.of(
                Map.of("field", "items[0].Player", "value", "Ichiro"),
                Map.of("field", "items[0].Hits", "value", 262),
                Map.of("field", "items[1].Player", "value", "Sisler"),
                Map.of("field", "items[1].Hits", "value", 257)))));

    Map<String, Object> alias = (Map<String, Object>) normalized.get("FilteredHits");
    List<Object> rows = (List<Object>) alias.get("rows");
    assertThat(rows)
        .containsExactly(
            Map.of("Player", "Ichiro", "Hits", 262),
            Map.of("Player", "Sisler", "Hits", 257));
    assertThat(alias).containsEntry("Player", "Ichiro").containsEntry("Hits", 262);
  }

  @Test
  void normalizesFlattenedRowsRowsToObjectRows() {
    Map<String, Object> normalized = normalizer.normalize(Map.of(
        "FilteredHits",
        Map.of(
            "rows",
            List.of(
                Map.of("field", "rows[0].Player", "value", "Ichiro"),
                Map.of("field", "rows[0].Hits", "value", 262)))));

    Map<String, Object> alias = (Map<String, Object>) normalized.get("FilteredHits");
    List<Object> rows = (List<Object>) alias.get("rows");
    assertThat(rows).containsExactly(Map.of("Player", "Ichiro", "Hits", 262));
  }

  @Test
  void normalizesNestedArraysAndObjectsRecursively() {
    Map<String, Object> normalized = normalizer.normalize(Map.of(
        "IcedCoffee",
        Map.of(
            "rows",
            List.of(
                Map.of("field", "items[0].title", "value", "Iced Coffee"),
                Map.of("field", "items[0].ingredients[0]", "value", "Coffee"),
                Map.of("field", "items[0].ingredients[1]", "value", "Ice"),
                Map.of("field", "items[0].nutrition.calories", "value", 120),
                Map.of("field", "items[0].variants[0].name", "value", "Small"),
                Map.of("field", "items[0].variants[0].prices[0].amount", "value", 2.5)))));

    Map<String, Object> alias = (Map<String, Object>) normalized.get("IcedCoffee");
    List<?> rows = (List<?>) alias.get("rows");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0)).isEqualTo(
        Map.of(
            "title", "Iced Coffee",
            "ingredients", List.of("Coffee", "Ice"),
            "nutrition", Map.of("calories", 120),
            "variants", List.of(Map.of("name", "Small", "prices", List.of(Map.of("amount", 2.5))))));
    assertThat(alias).containsEntry("title", "Iced Coffee");
  }

  @Test
  void copiesItemsArrayToRowsWhenRowsMissing() {
    Map<String, Object> normalized = normalizer.normalize(Map.of(
        "Alias",
        Map.of("items", List.of(Map.of("name", "A"), Map.of("name", "B")))));

    Map<String, Object> alias = (Map<String, Object>) normalized.get("Alias");
    assertThat(alias).containsEntry("name", "A");
    List<Object> rows = (List<Object>) alias.get("rows");
    assertThat(rows).containsExactly(Map.of("name", "A"), Map.of("name", "B"));
  }

  @Test
  void exposesFirstRowFieldsWithoutOverwritingExistingAliasFields() {
    Map<String, Object> normalized = normalizer.normalize(Map.of(
        "Alias",
        Map.of(
            "Player", "Manual value",
            "rows", List.of(Map.of("Player", "Ichiro", "Hits", 262)))));

    Map<String, Object> alias = (Map<String, Object>) normalized.get("Alias");
    assertThat(alias).containsEntry("Player", "Manual value").containsEntry("Hits", 262);
  }

  @Test
  void doesNotMutateInputContext() {
    Map<String, Object> alias = new LinkedHashMap<>();
    alias.put("rows", List.of(Map.of("field", "items[0].Player", "value", "Ichiro")));
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("FilteredHits", alias);

    Map<String, Object> normalized = normalizer.normalize(context);

    assertThat(context).isEqualTo(Map.of("FilteredHits", alias));
    assertThat(normalized).isNotSameAs(context);
  }
}
