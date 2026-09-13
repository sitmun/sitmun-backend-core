package org.sitmun.administration.controller;

import org.sitmun.administration.controller.dto.ServiceCapabilitiesRequest;
import org.sitmun.administration.service.extractor.capabilities.ExtractedMetadata;
import org.sitmun.administration.service.extractor.capabilities.ServiceCapabilitiesProbeService;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@BasePathAwareController
public class ServiceCapabilitiesExtractorController {

  private final ServiceCapabilitiesProbeService probeService;

  public ServiceCapabilitiesExtractorController(
      @NonNull ServiceCapabilitiesProbeService probeService) {
    this.probeService = probeService;
  }

  /**
   * Retrieve a service description document from a form overlay.
   *
   * @param request unsaved or saved service fields used to build the origin GetCapabilities request
   * @return 200 if a document is found; client should check this document or 400 if a document
   *     cannot be retrieved
   */
  @PostMapping("/helpers/capabilities")
  @ResponseBody
  public ResponseEntity<ExtractedMetadata> extractCapabilities(
      @RequestBody ServiceCapabilitiesRequest request) {
    ExtractedMetadata capabilities = probeService.probe(request);
    if (Boolean.TRUE.equals(capabilities.getSuccess())) {
      return ResponseEntity.ok(capabilities);
    }
    return ResponseEntity.badRequest().body(capabilities);
  }
}
