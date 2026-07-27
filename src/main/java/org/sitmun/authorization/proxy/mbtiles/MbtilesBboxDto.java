package org.sitmun.authorization.proxy.mbtiles;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MbtilesBboxDto(
    @JsonProperty("minX") double minX,
    @JsonProperty("minY") double minY,
    @JsonProperty("maxX") double maxX,
    @JsonProperty("maxY") double maxY) {}
