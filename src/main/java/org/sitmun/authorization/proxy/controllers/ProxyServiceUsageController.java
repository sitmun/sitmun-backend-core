package org.sitmun.authorization.proxy.controllers;

import java.time.ZoneId;
import org.sitmun.domain.service.usage.ServiceUsageStore;
import org.sitmun.proxy.contract.ServiceUsageReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/proxy/service-usage")
public class ProxyServiceUsageController {

  private final ServiceUsageStore store;
  private final ZoneId zone;

  @Autowired
  public ProxyServiceUsageController(ServiceUsageStore store) {
    this(store, ZoneId.systemDefault());
  }

  ProxyServiceUsageController(ServiceUsageStore store, ZoneId zone) {
    this.store = store;
    this.zone = zone;
  }

  @PostMapping
  public ResponseEntity<Void> ingest(@RequestBody ServiceUsageReport report) {
    store.addReport(report, zone);
    return ResponseEntity.noContent().build();
  }
}
