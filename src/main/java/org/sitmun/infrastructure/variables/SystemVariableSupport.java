package org.sitmun.infrastructure.variables;

import java.util.ArrayList;
import java.util.List;
import org.sitmun.domain.application.Application;
import org.sitmun.domain.territory.Territory;

/** SpEL helpers for system-variable YAML that cannot be a one-property path. */
public final class SystemVariableSupport {

  private SystemVariableSupport() {}

  /**
   * SITMUN 2 {@code MUN_INES}: current territory code, plus member codes when the application
   * unfolds children ({@code accessChildrenTerritory}).
   */
  public static String munInes(Application application, Territory territory) {
    if (territory == null || territory.getCode() == null) {
      return "";
    }
    String self = territory.getCode();
    if (application == null || !Boolean.TRUE.equals(application.getAccessChildrenTerritory())) {
      return self;
    }
    List<String> codes = new ArrayList<>();
    codes.add(self);
    if (territory.getMembers() != null) {
      territory.getMembers().stream()
          .map(Territory::getCode)
          .filter(code -> code != null && !code.isBlank() && !self.equals(code))
          .distinct()
          .sorted()
          .forEach(codes::add);
    }
    return String.join(",", codes);
  }
}
