package org.sitmun.authorization.proxy.mbtiles;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MbtilesServiceRefDto(
    @NotNull @JsonProperty("serviceId") Integer serviceId,
    @NotEmpty @JsonProperty("layerIds") List<Integer> layerIds) {}
