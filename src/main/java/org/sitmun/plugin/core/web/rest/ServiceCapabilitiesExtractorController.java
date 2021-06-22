package org.sitmun.plugin.core.web.rest;

import org.sitmun.plugin.core.service.ServiceCapabilities;
import org.sitmun.plugin.core.service.ServiceCapabilitiesExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Iterator;
import java.util.List;

@BasePathAwareController
public class ServiceCapabilitiesExtractorController {

  private final List<ServiceCapabilitiesExtractor> extractors;

  @Autowired
  public ServiceCapabilitiesExtractorController(@NonNull List<ServiceCapabilitiesExtractor> extractors) {
    this.extractors = extractors;
  }

  /**
   * Retrieve a service description document.
   *
   * @param url the url of the service description document.
   * @return 200 if a document is found; client should check this document or 400 if a document cannot be retrieved or the method is not found
   */
  @RequestMapping(method = RequestMethod.GET, path = "/helpers/capabilities")
  public @ResponseBody
  ResponseEntity<ServiceCapabilities> extractCapabilities(@RequestParam("url") String url) {
    Iterator<ServiceCapabilitiesExtractor> iterator = extractors.iterator();
    ServiceCapabilities capabilities = ServiceCapabilities.builder().success(false).reason("No available extractor").build();
    while (iterator.hasNext()) {
      ServiceCapabilitiesExtractor extractor = iterator.next();
      capabilities = extractor.extract(url);
      if (capabilities.getSuccess()) {
        return ResponseEntity.ok(capabilities);
      }
    }
    return ResponseEntity.badRequest().body(capabilities);
  }
}
