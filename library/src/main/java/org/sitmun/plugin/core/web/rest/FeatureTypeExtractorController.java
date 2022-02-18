package org.sitmun.plugin.core.web.rest;

import org.sitmun.plugin.core.service.ExtractedMetadata;
import org.sitmun.plugin.core.service.FeatureTypeExtractor;
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
public class FeatureTypeExtractorController {

  private final List<FeatureTypeExtractor> extractors;

  @Autowired
  public FeatureTypeExtractorController(@NonNull List<FeatureTypeExtractor> extractors) {
    this.extractors = extractors;
  }

  /**
   * Retrieve a feature type description document.
   *
   * @param url the url of that returns the feature type description document.
   * @return 200 if a document is found; client should check this document or 400 if a document cannot be retrieved or the method is not found
   */
  @RequestMapping(method = RequestMethod.GET, path = "/helpers/feature-type")
  public @ResponseBody
  ResponseEntity<ExtractedMetadata> extractCapabilities(@RequestParam("url") String url) {
    Iterator<FeatureTypeExtractor> iterator = extractors.iterator();
    ExtractedMetadata featureType = ExtractedMetadata.builder().success(false).reason("No available extractor").build();
    while (iterator.hasNext()) {
      FeatureTypeExtractor extractor = iterator.next();
      featureType = extractor.extract(url);
      if (featureType.getSuccess()) {
        return ResponseEntity.ok(featureType);
      }
    }
    return ResponseEntity.badRequest().body(featureType);
  }
}
