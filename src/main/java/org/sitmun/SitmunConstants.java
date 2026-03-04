package org.sitmun;

/** Application-wide string constants shared across layers and tests. */
public final class SitmunConstants {

  /** {@code STM_CONF} key for the default language (e.g. {@code en}, {@code es}). */
  public static final String LANGUAGE_DEFAULT_CONF_KEY = "language.default";

  /** {@code ProfileDto.global} key under which the proxy middleware URL is published. */
  public static final String PROXY_CONF_KEY = "proxy";

  private SitmunConstants() {}
}
