package org.sitmun.administration.service.extractor.capabilities;

import okhttp3.Request;

public interface ServiceCapabilitiesExtractor {

  ExtractedMetadata extract(Request request);
}
