package org.sitmun.authorization.client.shorturl;

import org.springframework.stereotype.Component;

@Component("none")
public class NoneShortUrlProvider implements ShortUrlProvider {

  @Override
  public String id() {
    return "none";
  }

  @Override
  public ShortUrlOutcome shorten(String url) {
    return new ShortUrlOutcome.Original(url);
  }
}
