package org.sitmun.authorization.proxy.ogc;

import java.util.Set;

/** One cartography the caller may use on the requested service. */
public record CartographyGrant(Integer id, Set<String> layers, boolean hasParameters) {}
