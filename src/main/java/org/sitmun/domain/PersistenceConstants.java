package org.sitmun.domain;

/** Constants and definitions. */
public final class PersistenceConstants {

  /** Oracle-safe max for bounded VARCHAR free text / URLs / long values in this schema. */
  private static final int VARCHAR_MAX = 4000;

  /** Identifier for BCP 47 Language Tag */
  public static final int BCP47_LANGUAGE_TAG = 20;

  /** Machine and human-readable identifiers, code lists. */
  public static final int IDENTIFIER = 50;

  /** Titles. */
  public static final int TITLE = 50;

  /** Abstract or short description. */
  public static final int SHORT_DESCRIPTION = 250;

  /** Long free text (abstracts, other information, literal translations). */
  public static final int LONG_DESCRIPTION = VARCHAR_MAX;

  /** The literal representation of a value. */
  public static final int VALUE = 250;

  /** Long literal or serialized value payload (e.g. filter value lists). */
  public static final int LONG_VALUE = VARCHAR_MAX;

  /** URL. */
  public static final int URL = VARCHAR_MAX;

  private PersistenceConstants() {}
}
