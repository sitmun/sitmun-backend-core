package org.sitmun.domain.service;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sitmun.domain.service.usage.ServiceUsageQueries;
import org.sitmun.domain.service.usage.UsageOperation;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AffectedApplications {

  private final EntityManager entities;

  public AffectedApplications(EntityManager entities) {
    this.entities = entities;
  }

  @Transactional(readOnly = true)
  public List<AffectedApplication> list(Instant now) {
    LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
    String failing = ServiceHealthQuery.failingWhere("s");
    String query =
        String.join(
            " union ",
            configured("join layer.service s", "join layer.treeNodes node", failing),
            configured(
                "join layer.spatialSelectionService s", "join layer.treeNodes node", failing),
            background(failing),
            situation(failing),
            task(failing),
            usage(failing));
    @SuppressWarnings("unchecked")
    List<Object[]> rows =
        entities
            .createQuery(query)
            .setParameter("from", ServiceUsageQueries.thirtyDayStart(today))
            .setParameter("to", ServiceUsageQueries.thirtyDayEnd(today))
            .getResultList();
    return assemble(rows);
  }

  private static String configured(String serviceJoin, String nodeJoin, String failing) {
    return """
        select a.id, a.name, s.id, s.name, '', cast(0 as long)
        from Cartography layer
        %s
        %s
        join node.tree tree
        join tree.availableApplications placement
        join placement.application a
        where %s
        """
        .formatted(serviceJoin, nodeJoin, failing);
  }

  private static String background(String failing) {
    return """
        select a.id, a.name, s.id, s.name, '', cast(0 as long)
        from Cartography layer
        join layer.service s
        join layer.permissions permission
        join permission.backgrounds background
        join background.applications placement
        join placement.application a
        where %s
        """
        .formatted(failing);
  }

  private static String situation(String failing) {
    return """
        select a.id, a.name, s.id, s.name, '', cast(0 as long)
        from Cartography layer
        join layer.service s
        join layer.permissions permission
        join permission.applications a
        where %s
        """
        .formatted(failing);
  }

  private static String task(String failing) {
    return """
        select a.id, a.name, s.id, s.name, '', cast(0 as long)
        from TreeNode node
        join node.task task
        join task.service s
        join node.tree tree
        join tree.availableApplications placement
        join placement.application a
        where %s
        """
        .formatted(failing);
  }

  private static String usage(String failing) {
    return """
        select a.id, a.name, s.id, s.name, link.id.operation, sum(link.requests)
        from ServiceUsage link, Service s, Application a
        where s.id = link.id.serviceId
          and a.id = link.id.applicationId
          and link.id.usageDay >= :from
          and link.id.usageDay < :to
          and %s
        group by a.id, a.name, s.id, s.name, link.id.operation
        """
        .formatted(failing);
  }

  private static List<AffectedApplication> assemble(List<Object[]> rows) {
    Map<Integer, Acc> byApplication = new LinkedHashMap<>();
    for (Object[] row : rows) {
      Integer applicationId = ((Number) row[0]).intValue();
      String applicationName = (String) row[1];
      Integer serviceId = ((Number) row[2]).intValue();
      String serviceName = (String) row[3];
      String operation = (String) row[4];
      long requests = ((Number) row[5]).longValue();
      boolean counted = operation != null && !operation.isEmpty();
      if (counted && (!UsageOperation.countsAsRequest(operation) || requests <= 0)) {
        continue;
      }
      Acc acc = byApplication.computeIfAbsent(applicationId, id -> new Acc(id, applicationName));
      acc.services.putIfAbsent(serviceId, serviceName);
      if (counted) {
        acc.requests += requests;
      }
    }
    List<AffectedApplication> applications = new ArrayList<>();
    for (Acc acc : byApplication.values()) {
      List<AffectedApplication.FailingService> services =
          acc.services.entrySet().stream()
              .map(
                  entry -> new AffectedApplication.FailingService(entry.getKey(), entry.getValue()))
              .sorted(
                  Comparator.comparing(AffectedApplication.FailingService::name)
                      .thenComparing(AffectedApplication.FailingService::id))
              .toList();
      AffectedApplication.AffectedUsage usage =
          acc.requests > 0
              ? AffectedApplication.AffectedUsage.used
              : AffectedApplication.AffectedUsage.configured;
      applications.add(new AffectedApplication(acc.id, acc.name, services, acc.requests, usage));
    }
    applications.sort(
        Comparator.comparingLong(AffectedApplication::requests30d)
            .reversed()
            .thenComparing(AffectedApplication::applicationName)
            .thenComparing(AffectedApplication::applicationId));
    return List.copyOf(applications);
  }

  private static final class Acc {
    private final Integer id;
    private final String name;
    private final Map<Integer, String> services = new LinkedHashMap<>();
    private long requests;

    private Acc(Integer id, String name) {
      this.id = id;
      this.name = name;
    }
  }
}
