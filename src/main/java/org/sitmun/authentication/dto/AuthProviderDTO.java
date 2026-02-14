package org.sitmun.authentication.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record AuthProviderDTO(String id, List<OidcProviderDTO> providers) {}
