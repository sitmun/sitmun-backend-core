package org.sitmun.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import java.util.Set;
import org.sitmun.upstream.signal.ServiceStatuses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ServiceHealthQuery {

  static final int REACHED_RANK = ServiceStatuses.REACHED.rank();
  private static final Set<String> HEALTH = Set.of("failing", "healthy", "unchecked");
  private static final Set<String> SORTS = Set.of("name", "type", "serviceURL", "id");

  static String failingWhere(String serviceAlias) {
    return latestRank(serviceAlias) + " > " + REACHED_RANK;
  }

  private static String latestRank(String serviceAlias) {
    return """
        (select c.statusRank from ServiceCheck c
          where c.service = %s
            and c.id = (
              select max(c3.id) from ServiceCheck c3
              where c3.service = %s
                and c3.observedAt = (
                  select max(c2.observedAt) from ServiceCheck c2 where c2.service = %s)))
        """
        .formatted(serviceAlias, serviceAlias, serviceAlias);
  }

  private final EntityManager entities;

  public ServiceHealthQuery(EntityManager entities) {
    this.entities = entities;
  }

  @Transactional(readOnly = true)
  public Page<Service> page(String health, String text, Pageable pageable) {
    String healthClause = healthClause(health);
    String textClause = textClause(text);
    String order = order(pageable.getSort());
    String where = " where " + healthClause + " and " + textClause;
    TypedQuery<Long> count =
        entities.createQuery("select count(s) from Service s" + where, Long.class);
    TypedQuery<Service> rows =
        entities.createQuery("select s from Service s" + where + order, Service.class);
    bindText(count, text);
    bindText(rows, text);
    long total = count.getSingleResult();
    if (pageable.isPaged()) {
      rows.setFirstResult((int) pageable.getOffset());
      rows.setMaxResults(pageable.getPageSize());
    }
    List<Service> content = rows.getResultList();
    return new PageImpl<>(content, pageable.isPaged() ? pageable : Pageable.unpaged(), total);
  }

  private static String healthClause(String health) {
    if (health == null || health.isBlank()) {
      return "1 = 1";
    }
    if (!HEALTH.contains(health)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "health");
    }
    return switch (health) {
      case "failing" -> failingWhere("s");
      case "healthy" -> latestRank("s") + " <= " + REACHED_RANK;
      case "unchecked" -> "not exists (select c0.id from ServiceCheck c0 where c0.service = s)";
      default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "health");
    };
  }

  private static String textClause(String text) {
    if (text == null || text.isBlank()) {
      return "1 = 1";
    }
    return """
        (lower(s.name) like lower(concat('%', :q, '%'))
          or lower(s.serviceURL) like lower(concat('%', :q, '%'))
          or lower(s.type) like lower(concat('%', :q, '%')))
        """;
  }

  private static void bindText(TypedQuery<?> query, String text) {
    if (text != null && !text.isBlank()) {
      query.setParameter("q", text);
    }
  }

  private static String order(Sort sort) {
    if (sort == null || sort.isUnsorted()) {
      return " order by s.id asc";
    }
    Sort.Order accessRank =
        sort.stream()
            .filter(order -> "accessRank".equals(order.getProperty()))
            .findFirst()
            .orElse(null);
    if (accessRank != null) {
      return " order by "
          + latestRank("s")
          + (accessRank.isDescending() ? " desc" : " asc")
          + " nulls last, s.id asc";
    }
    Sort.Order order = sort.iterator().next();
    if (!SORTS.contains(order.getProperty())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sort");
    }
    String property =
        "id".equals(order.getProperty()) ? "s.id" : "lower(s." + order.getProperty() + ")";
    return " order by " + property + (order.isDescending() ? " desc" : " asc") + ", s.id asc";
  }
}
