package org.sitmun.authorization.proxy.mbtiles;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Canonical tile request rebuilt from authorized persisted services (no MBTiles host). */
public record CanonicalTileRequestDto(
    @JsonProperty("mapServices") List<CanonicalMapServiceDto> mapServices,
    @JsonProperty("bbox") CanonicalBboxDto bbox,
    @JsonProperty("minZoom") int minZoom,
    @JsonProperty("maxZoom") int maxZoom) {

  public record CanonicalMapServiceDto(
      @JsonProperty("url") String url,
      @JsonProperty("layers") List<String> layers,
      @JsonProperty("type") String type) {}

  public record CanonicalBboxDto(
      @JsonProperty("minX") double minX,
      @JsonProperty("minY") double minY,
      @JsonProperty("maxX") double maxX,
      @JsonProperty("maxY") double maxY,
      @JsonProperty("srs") String srs) {}
}
