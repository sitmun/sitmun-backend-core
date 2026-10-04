package org.sitmun.authorization.client.shorturl;

import org.sitmun.authorization.client.service.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShortUrlService {

  private final AuthorizationService authorizationService;
  private final ShortUrlProvider provider;

  public ShortUrlService(
      AuthorizationService authorizationService, SelectedShortUrlProvider provider) {
    this.authorizationService = authorizationService;
    this.provider = provider.provider();
  }

  public String shorten(String username, String rawUrl, String requestHost) {
    ViewerMapUrl map =
        ViewerMapUrl.parse(rawUrl, requestHost)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL is not a viewer map"));
    if (authorizationService
        .findApplicationByUserApplicationAndTerritory(
            username, map.applicationId(), map.territoryId())
        .isEmpty()) {
      throw new AccessDeniedException("Access denied");
    }
    ShortUrlOutcome outcome = provider.shorten(rawUrl);
    if (outcome instanceof ShortUrlOutcome.Original original) {
      return original.url();
    }
    if (outcome instanceof ShortUrlOutcome.Shortened shortened) {
      return shortened.url();
    }
    if (outcome instanceof ShortUrlOutcome.Failed failed) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, failed.message());
    }
    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Short URL provider failed");
  }
}
