package org.sitmun.administration.service.access;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.sitmun.domain.service.check.ServiceCheckRepository;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ServiceCheckDueQuery {

  private final ServiceCheckRepository checks;

  public ServiceCheckDueQuery(ServiceCheckRepository checks) {
    this.checks = checks;
  }

  @Transactional(readOnly = true)
  public Optional<DueService> first(Instant dueBefore, Predicate<DueService> eligible) {
    try (Stream<DueService> due = checks.streamDue(ServiceCheckStore.BACKEND, dueBefore)) {
      return due.filter(eligible).findFirst();
    }
  }
}
