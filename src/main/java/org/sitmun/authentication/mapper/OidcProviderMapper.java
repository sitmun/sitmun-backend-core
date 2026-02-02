package org.sitmun.authentication.mapper;

import org.mapstruct.Mapper;
import org.sitmun.authentication.dto.OidcProviderDTO;
import org.sitmun.infrastructure.security.config.OidcAuthenticationProperties;

@Mapper(componentModel = "spring")
public interface OidcProviderMapper {

  OidcProviderDTO toRepresentationDTO(OidcAuthenticationProperties.ProviderConfig properties);

}
