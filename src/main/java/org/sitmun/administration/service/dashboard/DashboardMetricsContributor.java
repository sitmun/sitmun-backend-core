package org.sitmun.administration.service.dashboard;

import io.micrometer.core.instrument.MultiGauge;
import io.micrometer.core.instrument.Tags;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;

import java.sql.Date;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface DashboardMetricsContributor extends Runnable {
  default MultiGauge.Row<Number> getDateNumberRow(Object[] entry) {
    if (entry == null) {
      return null;
    }
    if (entry.length == 4 && Arrays.stream(entry).allMatch(Objects::nonNull)) {
      String key = MessageFormat.format("{0,number,0000}-{1,number,00}-{2,number,00}", entry[0], entry[1], entry[2]);
      Number value = (Number) entry[3];
      return MultiGauge.Row.of(Tags.of(DashboardInfoContributor.TAG, key), value);
    }
    if (entry.length == 3 && Arrays.stream(entry).allMatch(Objects::nonNull)) {
      String key = MessageFormat.format("{0,number,0000}-{1,number,00}", entry[0], entry[1]);
      Number value = (Number) entry[2];
      return MultiGauge.Row.of(Tags.of(DashboardInfoContributor.TAG, key), value);
    }
    return null;
  }

  /**
   * This method is used to convert the result of a query to a MultiGauge.Row&lt;Number> object.
   * The expected format of the query result is an array of 3 elements:
   * <ul>
   *  <li>the first element is the key of the row, that must be unique.</li>
   *  <li>the second element is the label of the row, that must be unique.</li>
   *  <li>the third element is the value of the row.</li>
   * </ul>
   */
  default MultiGauge.Row<Number> processIdLabelValue(Object[] entry) {
    if (entry != null && entry.length == 3 && Arrays.stream(entry).allMatch(Objects::nonNull)) {
      String label = (String) entry[1];
      Number value = (Number) entry[2];
      return MultiGauge.Row.of(Tags.of(DashboardInfoContributor.TAG, label), value);
    }
    return null;
  }

  default void addSinceDateParameter(Query query, int size) {
    if (size > 0) {
      boolean hasSinceDate = query.getParameters().stream().map(Parameter::getName).collect(Collectors.toSet()).contains("sinceDate");
      if (hasSinceDate) {
        query.setParameter("sinceDate", Date.valueOf(LocalDate.now().minusDays(size)));
      }
    }
  }

  default void runQuery(MultiGauge gauge, EntityManager entityManager, DashboardProperties.MetricDefinition definition) {
    if (definition.getSize() > 0) {
      Query query = entityManager.createQuery(definition.getQuery());
      addSinceDateParameter(query, definition.getSize());
      List<Object[]> results = query.getResultList();
      Iterable<MultiGauge.Row<?>> list = results.stream()
        .map(this::getDateNumberRow)
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableList());
      gauge.register(list);
    }
  }

}
