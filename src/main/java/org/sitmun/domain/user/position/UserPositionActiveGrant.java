package org.sitmun.domain.user.position;

/**
 * JPQL conjunct for a {@code UserConfiguration uc} alias.
 *
 * <p>Named {@code hasActivePosition(user, grantTerritory)} in the #184 plan. Evaluates the interval
 * on the grant territory. Exempts usernames {@code public} and {@code admin}. Inclusive last civil
 * day in the JVM default zone.
 */
public final class UserPositionActiveGrant {

  private UserPositionActiveGrant() {}

  public static final String ON_GRANT =
      """
       and (uc.user.username in ('public', 'admin') or exists (
         select 1 from UserPosition pos
         where pos.user = uc.user and pos.territory = uc.territory
           and (pos.createdDate is null or cast(pos.createdDate as date) <= current_date)
           and (pos.expirationDate is null or current_date <= cast(pos.expirationDate as date))))
      """;
}
