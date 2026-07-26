package org.sitmun.authorization.proxy.mbtiles;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MbtilesProxyRequestDto(
    @NotNull @JsonProperty("appId") Integer appId,
    @NotNull @JsonProperty("territoryId") Integer territoryId,
    @NotBlank @JsonProperty("action") String action,
    @JsonProperty("jobHandle") String jobHandle,
    @JsonProperty("bbox") MbtilesBboxDto bbox,
    @JsonProperty("minZoom") Integer minZoom,
    @JsonProperty("maxZoom") Integer maxZoom,
    @JsonProperty("srs") String srs,
    @JsonProperty("services") List<MbtilesServiceRefDto> services) {}
