package org.sitmun.authorization.proxy.ogc;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.sitmun.authorization.proxy.ogc.OgcRequestNames.Operation;
import org.sitmun.authorization.proxy.ogc.OgcRequestNames.View;

/** Decides whether an OGC request may use the cartographies the caller can reach. */
public final class OgcLayerAccess {

  private OgcLayerAccess() {}

  public static boolean allowService(View view, List<CartographyGrant> grants) {
    if (view.rejected() || view.operation() == Operation.TRANSACTION) {
      return false;
    }
    Set<CartographyGrant> involved = new LinkedHashSet<>();
    if (!view.layers().isEmpty()) {
      if (allowsSubset(view.operation())) {
        if (!cover(view.layers(), grants, involved)) {
          return false;
        }
      } else {
        CartographyGrant match = exact(view.layers(), grants);
        if (match == null) {
          return false;
        }
        involved.add(match);
      }
    }
    if (!view.queryLayers().isEmpty()) {
      if (!view.layers().isEmpty() && !view.layers().containsAll(view.queryLayers())) {
        return false;
      }
      if (!cover(view.queryLayers(), grants, involved)) {
        return false;
      }
    }
    if (!view.legendLayers().isEmpty() && !cover(view.legendLayers(), grants, involved)) {
      return false;
    }
    if (!view.typeNames().isEmpty() && !cover(view.typeNames(), grants, involved)) {
      return false;
    }
    if (failClosed(view) && involved.stream().anyMatch(CartographyGrant::hasParameters)) {
      return false;
    }
    return true;
  }

  public static boolean allowCartography(View view, CartographyGrant grant) {
    if (view.rejected()) {
      return false;
    }
    Set<String> owned = grant.layers();
    if (!view.layers().isEmpty()) {
      if (!owned.containsAll(view.layers())) {
        return false;
      }
      if (!allowsSubset(view.operation()) && !owned.equals(view.layers())) {
        return false;
      }
    }
    if (!view.queryLayers().isEmpty()) {
      if (!owned.containsAll(view.queryLayers())) {
        return false;
      }
      if (!view.layers().isEmpty() && !view.layers().containsAll(view.queryLayers())) {
        return false;
      }
    }
    if (!view.legendLayers().isEmpty() && !owned.containsAll(view.legendLayers())) {
      return false;
    }
    if (view.operation() == Operation.TRANSACTION) {
      return !view.typeNames().isEmpty() && owned.containsAll(view.typeNames());
    }
    return view.typeNames().isEmpty() || owned.containsAll(view.typeNames());
  }

  private static boolean allowsSubset(Operation operation) {
    return operation == Operation.GET_FEATURE_INFO || operation == Operation.DESCRIBE_LAYER;
  }

  private static boolean failClosed(View view) {
    return switch (view.operation()) {
      case GET_MAP, GET_FEATURE_INFO, GET_FEATURE, DESCRIBE_LAYER -> true;
      case OTHER -> !view.layers().isEmpty();
      default -> false;
    };
  }

  private static boolean cover(
      Set<String> names, List<CartographyGrant> grants, Set<CartographyGrant> involved) {
    for (String name : names) {
      List<CartographyGrant> owners =
          grants.stream().filter(grant -> grant.layers().contains(name)).toList();
      if (owners.isEmpty()) {
        return false;
      }
      involved.addAll(owners);
    }
    return true;
  }

  private static CartographyGrant exact(Set<String> names, List<CartographyGrant> grants) {
    for (CartographyGrant grant : grants) {
      if (grant.layers().equals(names)) {
        return grant;
      }
    }
    return null;
  }
}
