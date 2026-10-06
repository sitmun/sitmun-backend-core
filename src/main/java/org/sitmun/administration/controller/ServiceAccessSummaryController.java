package org.sitmun.administration.controller;

import java.time.Instant;
import java.util.List;
import org.sitmun.administration.service.access.ServiceAccessSamples;
import org.sitmun.administration.service.access.ServiceAccessSummaries;
import org.sitmun.administration.service.access.ServiceAccessSummary;
import org.sitmun.administration.service.access.ServiceAccessTrend;
import org.sitmun.administration.service.access.ServiceCheckProperties;
import org.sitmun.domain.service.AffectedApplication;
import org.sitmun.domain.service.AffectedApplications;
import org.sitmun.domain.service.usage.ServiceUsageQueries;
import org.sitmun.domain.service.usage.ServiceUsageView;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@BasePathAwareController
public class ServiceAccessSummaryController {

  private final ServiceAccessSummaries summaries;
  private final ServiceUsageQueries usage;
  private final ServiceCheckProperties serviceCheck;
  private final AffectedApplications affectedApplications;

  public ServiceAccessSummaryController(
      ServiceAccessSummaries summaries,
      ServiceUsageQueries usage,
      ServiceCheckProperties serviceCheck,
      AffectedApplications affectedApplications) {
    this.summaries = summaries;
    this.usage = usage;
    this.serviceCheck = serviceCheck;
    this.affectedApplications = affectedApplications;
  }

  @GetMapping("/services/access-summaries")
  @ResponseBody
  public List<ServiceAccessSummary> accessSummaries() {
    return summaries.list(Instant.now());
  }

  @GetMapping("/services/affected-applications")
  @ResponseBody
  public List<AffectedApplication> affectedApplications() {
    return affectedApplications.list(Instant.now());
  }

  @GetMapping("/services/{id}/access-trend")
  @ResponseBody
  public ServiceAccessTrend accessTrend(
      @PathVariable Integer id, @RequestParam("bucket") String bucket) {
    if (!"10m".equals(bucket)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    return new ServiceAccessTrend(summaries.tenMinuteBuckets(id, Instant.now()));
  }

  @GetMapping("/services/{id}/access-samples")
  @ResponseBody
  public ServiceAccessSamples accessSamples(@PathVariable Integer id) {
    return summaries.samples(
        id, Instant.now(), serviceCheck.timeout(), serviceCheck.sampleRetention());
  }

  @GetMapping("/services/{id}/usage")
  @ResponseBody
  public ServiceUsageView usage(@PathVariable Integer id, @RequestParam("range") String range) {
    return usage.view(id, range, Instant.now());
  }
}
