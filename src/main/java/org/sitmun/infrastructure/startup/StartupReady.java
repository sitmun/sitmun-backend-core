package org.sitmun.infrastructure.startup;

import org.springframework.stereotype.Component;

/** Tracks whether startup tasks (e.g. CodeListValueSetup) have completed. Used for readiness. */
@Component
public class StartupReady {

  private volatile boolean ready = false;

  public boolean isReady() {
    return ready;
  }

  public void setReady(boolean ready) {
    this.ready = ready;
  }
}
