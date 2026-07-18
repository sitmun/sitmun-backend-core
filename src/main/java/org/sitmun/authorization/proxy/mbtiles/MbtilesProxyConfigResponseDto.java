package org.sitmun.authorization.proxy.mbtiles;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MbtilesProxyConfigResponseDto(
    @JsonProperty("appId") int appId,
    @JsonProperty("territoryId") int territoryId,
    @JsonProperty("action") String action,
    @JsonProperty("username") String username,
    @JsonProperty("exp") long exp,
    @JsonProperty("tileRequest") CanonicalTileRequestDto tileRequest) {}
