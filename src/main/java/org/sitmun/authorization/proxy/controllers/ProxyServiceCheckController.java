package org.sitmun.authorization.proxy.controllers;

import java.util.Optional;
import org.sitmun.administration.service.access.ServiceAccessCheckExecutor;
import org.sitmun.administration.service.access.ServiceCheckProperties;
import org.sitmun.domain.service.Service;
import org.sitmun.domain.service.ServiceRepository;
import org.sitmun.domain.service.check.ServiceCheckStore;
import org.sitmun.proxy.contract.ServiceCheckReport;
import org.sitmun.upstream.signal.Classification;
import org.sitmun.upstream.signal.ServiceCheckClassifier;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/proxy/service-checks")
public class ProxyServiceCheckController {

  private final ServiceRepository serviceRepository;
  private final ServiceCheckStore store;
  private final ServiceCheckClassifier classifier;
  private final ServiceCheckProperties properties;

  public ProxyServiceCheckController(
      ServiceRepository serviceRepository,
      ServiceCheckStore store,
      ServiceCheckClassifier classifier,
      ServiceCheckProperties properties) {
    this.serviceRepository = serviceRepository;
    this.store = store;
    this.classifier = classifier;
    this.properties = properties;
  }

  @PostMapping
  public ResponseEntity<Void> ingest(@RequestBody ServiceCheckReport report) {
    Optional<Classification> classification =
        classifier.classify(report.requestContext(), report.exchangeSignals());
    if (classification.isEmpty()) {
      return ResponseEntity.noContent().build();
    }
    int serviceId = Math.toIntExact(report.serviceId());
    Service service =
        serviceRepository
            .findById(serviceId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Service " + serviceId + " not found"));
    Classification found = classification.get();
    store.record(
        service,
        ServiceCheckStore.PROXY,
        found.status(),
        report.elapsedMs(),
        ServiceAccessCheckExecutor.detail(
            report.requestContext(), found.evidence(), properties.detailMaxLength()),
        report.observedAt());
    return ResponseEntity.noContent().build();
  }
}
