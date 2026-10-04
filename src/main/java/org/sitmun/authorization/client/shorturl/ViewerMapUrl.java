package org.sitmun.authorization.client.shorturl;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A viewer map URL on this deployment, with the application and territory in the route. */
public record ViewerMapUrl(int applicationId, int territoryId) {

  private static final Pattern ROUTE =
      Pattern.compile("(?:^|/)(?:public/map|user/map|embedded-map)/(\\d+)/(\\d+)(?:/|\\?|$)");

  public static Optional<ViewerMapUrl> parse(String rawUrl, String requestHost) {
    if (rawUrl == null || rawUrl.isBlank() || requestHost == null || requestHost.isBlank()) {
      return Optional.empty();
    }
    URI uri;
    try {
      uri = URI.create(rawUrl);
    } catch (IllegalArgumentException exception) {
      return Optional.empty();
    }
    if (uri.getHost() == null || !uri.getHost().equalsIgnoreCase(requestHost)) {
      return Optional.empty();
    }
    String route = uri.getRawFragment();
    if (route == null || route.isBlank()) {
      route = uri.getRawPath();
    }
    if (route == null) {
      return Optional.empty();
    }
    Matcher matcher = ROUTE.matcher(route);
    if (!matcher.find()) {
      return Optional.empty();
    }
    return Optional.of(
        new ViewerMapUrl(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
  }
}
