package org.sitmun.authorization.client.shorturl;

public interface ShortUrlProvider {

  String id();

  ShortUrlOutcome shorten(String url);
}
