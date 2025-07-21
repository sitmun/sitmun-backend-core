package org.sitmun.administration.controller;

import java.util.Iterator;
import java.util.List;
import org.sitmun.administration.service.extractor.capabilities.ExtractedMetadata;
import org.sitmun.administration.service.extractor.capabilities.ServiceCapabilitiesExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@BasePathAwareController
public class ServiceCapabilitiesExtractorController {

  private final List<ServiceCapabilitiesExtractor> extractors;

  @Autowired
  public ServiceCapabilitiesExtractorController(
      @NonNull List<ServiceCapabilitiesExtractor> extractors) {
    this.extractors = extractors;
  }

  /**
   * Retrieve a service description document.
   *
   * @param url the url of the service description document.
   * @return 200 if a document is found; client should check this document or 400 if a document
   *     cannot be retrieved or the method is not found
   */
  @GetMapping("/helpers/capabilities")
  @ResponseBody
  public ResponseEntity<ExtractedMetadata> extractCapabilities(@RequestParam("url") String url) {
    Iterator<ServiceCapabilitiesExtractor> iterator = extractors.iterator();
    ExtractedMetadata capabilities =
        ExtractedMetadata.builder().success(false).reason("No available extractor").build();
    while (iterator.hasNext()) {
      ServiceCapabilitiesExtractor extractor = iterator.next();
      capabilities = extractor.extract(url);
      if (Boolean.TRUE.equals(capabilities.getSuccess())) {
        return ResponseEntity.ok(capabilities);
      }
    }
    return ResponseEntity.badRequest().body(capabilities);
  }
}
