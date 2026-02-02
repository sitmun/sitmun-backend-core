package org.sitmun.authentication.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AuthProviderDTO(String id, List<OidcProviderDTO> providers) {
}
