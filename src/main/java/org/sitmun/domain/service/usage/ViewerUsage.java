package org.sitmun.domain.service.usage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sitmun.domain.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ViewerUsage {

  private final ServiceUsageStore store;
  private final ZoneId zone;
  private final Object lock = new Object();
  private final Map<ServiceUsageKey, Long> pending = new HashMap<>();

  @Autowired
  public ViewerUsage(ServiceUsageStore store) {
    this(store, ZoneId.systemDefault());
  }

  ViewerUsage(ServiceUsageStore store, ZoneId zone) {
    this.store = store;
    this.zone = zone;
  }

  public void record(int applicationId, Collection<Service> services) {
    LocalDate day = LocalDate.now(zone);
    synchronized (lock) {
      for (Service service : services) {
        if (service == null || service.getId() == null) {
          continue;
        }
        ServiceUsageKey key =
            new ServiceUsageKey(service.getId(), applicationId, day, UsageOperation.VIEWER_CONFIG);
        pending.merge(key, 1L, Long::sum);
      }
    }
  }

  public void flush() {
    Map<ServiceUsageKey, Long> batch;
    synchronized (lock) {
      if (pending.isEmpty()) {
        return;
      }
      batch = new HashMap<>(pending);
    }
    for (Map.Entry<ServiceUsageKey, Long> entry : batch.entrySet()) {
      ServiceUsageKey key = entry.getKey();
      store.add(
          key.getServiceId(),
          key.getApplicationId(),
          key.getUsageDay(),
          key.getOperation(),
          entry.getValue(),
          0);
      synchronized (lock) {
        pending.computeIfPresent(
            key,
            (currentKey, current) -> {
              long left = current - entry.getValue();
              return left > 0 ? left : null;
            });
      }
    }
  }
}
