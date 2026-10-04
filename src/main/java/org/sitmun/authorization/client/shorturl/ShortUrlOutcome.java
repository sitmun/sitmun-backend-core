package org.sitmun.authorization.client.shorturl;

public sealed interface ShortUrlOutcome {

  record Original(String url) implements ShortUrlOutcome {}

  record Shortened(String url) implements ShortUrlOutcome {}

  record Failed(String message) implements ShortUrlOutcome {}
}
