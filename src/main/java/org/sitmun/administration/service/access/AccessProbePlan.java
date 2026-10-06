package org.sitmun.administration.service.access;

import java.net.URI;

public record AccessProbePlan(
    String protocol,
    String request,
    URI url,
    URI fallbackUrl,
    boolean interpretBody,
    Authorization authorization) {

  public sealed interface Authorization {
    record None() implements Authorization {}

    record Basic(String username, String password) implements Authorization {
      @Override
      public String toString() {
        return "Basic[username=" + username + "]";
      }
    }
  }

  @Override
  public String toString() {
    return "AccessProbePlan[protocol="
        + protocol
        + ", request="
        + request
        + ", url="
        + url
        + ", fallbackUrl="
        + fallbackUrl
        + ", interpretBody="
        + interpretBody
        + ", authorization="
        + authorization
        + "]";
  }
}
